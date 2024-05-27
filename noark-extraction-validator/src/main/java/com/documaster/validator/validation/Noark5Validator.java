/**
 * Noark Extraction Validator
 * Copyright (C) 2016, Documaster AS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.documaster.validator.validation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.documaster.validator.config.commands.Noark5Command;
import com.documaster.validator.converters.xsd.XsdConverter;
import com.documaster.validator.exceptions.ValidationException;
import com.documaster.validator.reporting.ReportFactory;
import com.documaster.validator.storage.core.Storage;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.storage.model.Field;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationResult;
import com.documaster.validator.validation.noark5.model.Noark5PackageEntity;
import com.documaster.validator.validation.noark5.model.Noark5PackageStructure;
import com.documaster.validator.validation.noark5.parsers.BaseHandler;
import com.documaster.validator.validation.noark5.parsers.HandlerFactory;
import com.documaster.validator.validation.noark5.provider.ValidationGroup;
import com.documaster.validator.validation.noark5.provider.ValidationProvider;
import com.documaster.validator.validation.noark5.provider.data.Data;
import com.documaster.validator.validation.noark5.provider.data.ValidationData;
import com.documaster.validator.validation.noark5.provider.rules.Check;
import com.documaster.validator.validation.noark5.provider.rules.Test;
import com.documaster.validator.validation.noark5.validators.XMLValidator;
import com.documaster.validator.validation.noark5.validators.XSDValidator;
import com.documaster.validator.validation.utils.ChecksumCalculator;
import com.documaster.validator.validation.utils.DefaultXMLHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public abstract class Noark5Validator<T extends Noark5Command> extends Validator<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Noark5Validator.class);

	private final XsdConverter converter = new XsdConverter();

	public Noark5Validator(T command) {

		super(command, new ValidationCollector());
	}

	@Override
	public ValidationCollector run() throws Exception {

		LOGGER.info("Executing Noark {} extraction validation ...", getCommand().getNoarkVersion());

		Noark5PackageStructure structure = null;

		try {

			// Storage won't initialize if database file is populated
			deleteDatabaseDirIfStorageTypeIsFile();

			structure = prepareStructure();
			validateStructure(structure);

			// Init storage
			Storage.init(getCommand().getStorageConfiguration(), getCommand().getProperties().getUniqueFieldsMap());
			Storage.get().connect();
			Storage.get().startWriter();

			// Persist the extraction package data
			convertXSDSchemas(structure);
			storeXSDSchemas();
			storeXMLFiles(structure);
			storePackageChecksums(structure);

			boolean isSuccessfulCompletion = Storage.get().stopWriter();
			if (!isSuccessfulCompletion) {
				throw new ValidationException("Data could not be persisted", Storage.get().getLastWriterException());
			}

			// Validate the extraction package data
			runValidationQueries();

			LOGGER.info("Noark {} extraction validation completed.", getCommand().getNoarkVersion());

		} catch (Exception ex) {

			LOGGER.error(String.format("Noark %s validation failed.", getCommand().getNoarkVersion()), ex);

			ValidationResult error = new ValidationResult(
					ValidationGroup.EXCEPTIONS.getNextGroupId(getCollector()), "Exceptions",
					"Unexpected errors that the validator could not recover from",
					ValidationGroup.EXCEPTIONS.getName());
			error.addError(new BaseItem().add("exception", ex.getMessage()));

			collect(error);

		} finally {

			if (structure != null) {

				FileUtils.deleteQuietly(structure.getNoarkSchemasDirectory());
			}

			try {
				// The code below could throw an Error and the file database (if present) will not get deleted
				ReportFactory.generateReports(getCommand(), getCollector(), getArchiveTitle());

				if (Storage.get() != null) {
					Storage.get().stopWriter();
					Storage.get().destroy();
				}
			} finally {

				if (!getCommand().getStorageConfiguration().getPreserveFileDb()) {

					deleteDatabaseDirIfStorageTypeIsFile();
				}
			}
		}

		return getCollector();
	}

	private void deleteDatabaseDirIfStorageTypeIsFile() throws Exception {

		if (getCommand().getStorageConfiguration().getStorageType() == Storage.StorageType.HSQLDB_FILE) {

			LOGGER.info("Deleting file database storage directory");

			FileUtils.deleteDirectory(new File(getCommand().getStorageConfiguration().getDatabaseDirLocation()));
		}
	}

	/**
	 * Prepares the {@link Noark5PackageStructure}.
	 */
	private Noark5PackageStructure prepareStructure() throws Exception {

		// Temporary directory for storing the original Noark 5.x schemas
		File tempNoarkSchemasDirectory = Files.createTempDirectory("noark-extraction-validator-").toFile();

		// Initialize the package structure
		Noark5PackageStructure structure = new Noark5PackageStructure(
				getCommand().getExtractionDirectory(), tempNoarkSchemasDirectory,
				getCommand().getCustomSchemaLocation());

		// Create temporary files containing the original Noark 5 schemas
		for (Noark5PackageEntity entity : structure.values()) {

			for (String xsdSchemaName : entity.getXsdShemasNames()) {

				String noarkSchemaLocation = getCommand().getDefaultSchemaLocation() + "/" + xsdSchemaName;
				File tempXsdFile = new File(tempNoarkSchemasDirectory, xsdSchemaName);

				FileUtils.copyInputStreamToFile(
						getClass().getClassLoader().getResourceAsStream(noarkSchemaLocation),
						tempXsdFile);
			}
		}

		return structure;
	}

	/**
	 * Validates the integrity of the extraction package structure.
	 */
	private void validateStructure(Noark5PackageStructure structure) throws Exception {

		boolean stopValidation = false;

		XMLValidator xmlValidator = new XMLValidator(getCollector());
		XSDValidator xsdValidator = new XSDValidator(getCollector());

		for (Noark5PackageEntity entity : structure.values()) {

			if (!entity.getXmlFile().isFile() && entity.isOptional()) {
				LOGGER.info("Did not validate missing optional XML entity {}", entity.getXmlFileName());
				continue;
			}

			// Validate the extraction-distributed XSD schemas
			for (File xsdSchema : entity.getPackageSchemas()) {
				xsdValidator.isValid(xsdSchema, getCommand().getProperties().getChecksumFor(xsdSchema.getName()));
			}

			// Validate the extraction-distributed XML files
			boolean isXMLValid = xmlValidator.isValid(entity, getCommand().getIgnoreNonCompliantXML());
			stopValidation = stopValidation || !isXMLValid;
		}

		if (stopValidation) {
			throw new ValidationException(
					"Validation has been stopped due to unrecoverable extraction integrity violation.");
		}
	}

	/**
	 * Converts the XSD Schemas to {@link Item}(s).
	 */
	private void convertXSDSchemas(Noark5PackageStructure structure) throws Exception {

		converter.convert(structure.getAllNoarkSchemaFiles());

		// Add additional fields
		for (ItemDef itemDef : converter.getItemDefs().values()) {

			Set<String> additionalFields = getCommand().getProperties().getExtraFieldsInTable(itemDef.getFullName());

			if (!additionalFields.isEmpty()) {
				for (String fieldName : additionalFields) {
					itemDef.addField(fieldName, Field.FieldType.STRING);
				}
			}
		}
	}

	/**
	 * Persists the structure of the converted {@link ItemDef}s within the specified
	 * {@link Storage.StorageType}.
	 */
	private void storeXSDSchemas() {

		LOGGER.info("Storing XSD schemas ...");

		// addml.unique element causes issues in hsqldb
		List<String> ignoredAddmlTables = Collections.singletonList("addml.unique");

		for (ItemDef itemDef : converter.getItemDefs().values()) {
			if (ignoredAddmlTables.contains(itemDef.getFullName())) {
				continue;
			}
			Storage.get().write(itemDef);
		}

		for (Item item : converter.getItems()) {
			Storage.get().write(item);
		}
	}

	/**
	 * Stores the data from the parsed XML files.
	 */
	private void storeXMLFiles(Noark5PackageStructure structure) throws Exception {

		LOGGER.info("Storing XML data and extracting document information ...");

		for (Noark5PackageEntity entity : structure.values()) {

			if (!entity.getXmlFile().isFile() && entity.isOptional()) {
				LOGGER.info("Did not persist missing optional XML entity {}", entity.getXmlFileName());
				continue;
			}

			DefaultXMLHandler exceptionHandler = new DefaultXMLHandler();
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();

			XMLReader reader = saxParser.getXMLReader();

			BaseHandler xmlHandler = HandlerFactory.createHandler(entity.getXmlFile(), reader, converter.getItemDefs());

			reader.setContentHandler(xmlHandler);
			reader.setErrorHandler(exceptionHandler);

			try (
					FileInputStream fis = new FileInputStream(entity.getXmlFile());
					BufferedInputStream bis = new BufferedInputStream(fis)) {
				reader.parse(new InputSource(bis));
			}

			if (exceptionHandler.hasExceptions()) {
				ValidationResult errorResult = new ValidationResult(
						xmlHandler.getValidationGroup().getNextGroupId(getCollector()), "Parse errors",
						"Exceptions that occurred while parsing the package XML files. Such exceptions might "
								+ "indicate an error in the validator itself and should be reported to its "
								+ "developers. Test results cannot be trusted upon such errors.",
						xmlHandler.getValidationGroup().getName());
				errorResult.addErrors(exceptionHandler.getExceptionsAsItems());
				collect(errorResult);
			}
		}
	}

	/**
	 * Retrieves the checksums of all entities in the extraction package and stores them in the addml.property {@link
	 * ItemDef}.
	 */
	private void storePackageChecksums(Noark5PackageStructure structure) throws Exception {

		for (Noark5PackageEntity entity : structure.values()) {

			if (!entity.getXmlFile().isFile() && entity.isOptional()) {
				LOGGER.info("Did not retrieve the checksum of missing optional XML entity {}", entity.getXmlFileName());
				continue;
			}

			storePackageEntityChecksum(entity.getXmlFile());

			for (File schema : entity.getPackageSchemas()) {
				storePackageEntityChecksum(schema);
			}
		}
	}

	private void storePackageEntityChecksum(File file) {

		Item itemChecksum = new Item(converter.getItemDefs().get("addml.property"));
		itemChecksum.add("name", file.getName());
		itemChecksum.add("value", ChecksumCalculator.getFileSha256Checksum(file));

		Storage.get().write(itemChecksum);
	}

	private void runValidationQueries() throws Exception {

		LOGGER.info("Validating extraction ...");

		String validationFileLocation = MessageFormat.format("noark5/{0}/{0}-validation.xml", getCommand().getName());

		JAXBContext jaxbContext = JAXBContext
				.newInstance(ValidationProvider.class, Data.class, ValidationData.class, ValidationGroup.class,
						Test.class, Check.class);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

		ValidationProvider vp;
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(validationFileLocation)) {
			vp = (ValidationProvider) jaxbUnmarshaller.unmarshal(is);
		}

		for (Check check : vp.getChecks()) {

			LOGGER.info(MessageFormat.format("Checking: {0} ...", check.getTitle()));

			String informationRequest = check.getData().getInfoRequest();

			ValidationResult result = new ValidationResult(
					check.getId(), check.getTitle(), check.getDescription(), check.getGroup().getName());

			if (!StringUtils.isBlank(informationRequest)) {
				result.addInformation(Storage.get().fetch(informationRequest));
			}

			collect(result);
		}

		for (Test test : vp.getTests()) {

			LOGGER.info(MessageFormat.format("Testing: {0} ...", test.getTitle()));

			String informationRequest = test.getData().getInfoRequest();
			String warningsRequest = test.getData().getWarningsRequest();
			String errorRequest = test.getData().getErrorsRequest();

			ValidationResult result = new ValidationResult(
					test.getId(), test.getTitle(), test.getDescription(), test.getGroup().getName());

			if (!StringUtils.isBlank(informationRequest)) {
				result.addInformation(Storage.get().fetch(informationRequest));
			}

			if (!StringUtils.isBlank(warningsRequest)) {
				result.addWarnings(Storage.get().fetch(warningsRequest));
			}

			if (!StringUtils.isBlank(errorRequest)) {
				result.addErrors(Storage.get().fetch(errorRequest));
			}

			collect(result);
		}
	}

	private String getArchiveTitle() {

		String archiveTitle = null;

		try {
			if (Storage.get() != null && Storage.get().isReadAvailable()) {
				List<BaseItem> entries = Storage.get()
						.fetch("SELECT value FROM addml.additionalelement WHERE name = 'archive';");

				if (entries != null && !entries.isEmpty()) {
					archiveTitle = entries.get(0).getValues().entrySet().iterator().next().getValue().toString();
				}
			}
		} catch (Exception ex) {
			LOGGER.warn("Could not fetch the archive's title");
		}

		return archiveTitle;
	}
}
