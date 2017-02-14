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
package com.documaster.validator.validation.noark53;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

import com.documaster.validator.config.commands.Noark53Command;
import com.documaster.validator.converters.xsd.XsdConverter;
import com.documaster.validator.exceptions.ValidationException;
import com.documaster.validator.reporting.ReportFactory;
import com.documaster.validator.storage.core.Storage;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.storage.model.Field;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.validation.Validator;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationResult;
import com.documaster.validator.validation.noark53.parsers.BaseHandler;
import com.documaster.validator.validation.noark53.parsers.HandlerFactory;
import com.documaster.validator.validation.noark53.provider.ValidationGroup;
import com.documaster.validator.validation.noark53.provider.data.Data;
import com.documaster.validator.validation.noark53.provider.data.ValidationData;
import com.documaster.validator.validation.noark53.provider.ValidationProvider;
import com.documaster.validator.validation.noark53.provider.rules.Check;
import com.documaster.validator.validation.noark53.provider.rules.Test;
import com.documaster.validator.validation.noark53.model.Noark53PackageEntity;
import com.documaster.validator.validation.noark53.model.Noark53PackageStructure;
import com.documaster.validator.validation.noark53.validators.XMLValidator;
import com.documaster.validator.validation.noark53.validators.XSDValidator;
import com.documaster.validator.validation.utils.ChecksumCalculator;
import com.documaster.validator.validation.utils.DefaultXMLHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class Noark53Validator extends Validator<Noark53Command> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Noark53Validator.class);

	private Noark53PackageStructure structure;

	private XsdConverter converter;

	public Noark53Validator(Noark53Command command) {

		super(command);
	}

	@Override
	public void run() throws Exception {

		LOGGER.info("Executing Noark 5.3 extraction validation ...");

		try {
			prepareStructure();
			validateStructure();

			// Init storage
			Storage.init(getCommand().getStorageConfiguration(), getCommand().getProperties().getUniqueFieldsMap());
			Storage.get().connect();
			Storage.get().startWriter();

			// Persist the extraction package data
			convertXSDSchemas();
			storeXSDSchemas();
			storeXMLFiles();
			storePackageChecksums();

			boolean isSuccessfulCompletion = Storage.get().stopWriter();
			if (!isSuccessfulCompletion) {
				throw new ValidationException("Data could not be persisted", Storage.get().getLastWriterException());
			}

			// Validate the extraction package data
			runValidationQueries();

			LOGGER.info("Noark 5.3 extraction validation completed.");

		} catch (Exception ex) {

			LOGGER.error("Noark 5.3 validation failed.", ex);

			ValidationResult error = new ValidationResult(
					ValidationGroup.EXCEPTIONS.getNextGroupId(), "Exceptions",
					"Unexpected errors that the validator could not recover from",
					ValidationGroup.EXCEPTIONS.getName());
			error.addError(new BaseItem().add("exception", ex.getMessage()));

			ValidationCollector.get().collect(error);

		} finally {

			FileUtils.deleteQuietly(structure.getNoarkSchemasDirectory());

			ReportFactory.generateReports(getCommand(), getArchiveTitle());

			if (Storage.get() != null) {
				Storage.get().stopWriter();
				Storage.get().destroy();
			}
		}
	}

	/**
	 * Prepares the {@link Noark53PackageStructure}.
	 */
	private void prepareStructure() throws IOException {

		// Temporary directory for storing the original Noark 5.3 schemas
		File tempNoarkSchemasDirectory = Files.createTempDirectory("noark-extraction-validator-").toFile();

		// Initialize the package structure
		structure = new Noark53PackageStructure(getCommand().getExtractionDirectory(), tempNoarkSchemasDirectory);

		// Create temporary files containing the original Noark 5.3 schemas
		for (Noark53PackageEntity entity : structure.values()) {

			for (String xsdSchemaName : entity.getXsdShemasNames()) {

				String noarkSchemaLocation = Noark53Command.COMMAND_NAME + "/" + xsdSchemaName;
				File tempXsdFile = new File(tempNoarkSchemasDirectory, xsdSchemaName);

				FileUtils.copyInputStreamToFile(
						getClass().getClassLoader().getResourceAsStream(noarkSchemaLocation),
						tempXsdFile);
			}
		}
	}

	/**
	 * Validates the integrity of the extraction package structure.
	 */
	private void validateStructure() throws Exception {

		boolean stopValidation = false;

		for (Noark53PackageEntity entity : structure.values()) {

			if (!entity.getXmlFile().isFile() && entity.isOptional()) {
				LOGGER.info("Did not validate missing optional XML entity {}", entity.getXmlFileName());
				continue;
			}

			// Validate the extraction-distributed XSD schemas
			for (File xsdSchema : entity.getPackageSchemas()) {
				XSDValidator.isValid(xsdSchema, getCommand().getProperties().getChecksumFor(xsdSchema.getName()));
			}

			// Validate the extraction-distributed XML files
			boolean isXMLValid = XMLValidator.isValid(entity, getCommand().getIgnoreNonCompliantXML());
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
	private void convertXSDSchemas() throws Exception {

		converter = new XsdConverter();
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
	private void storeXMLFiles() throws Exception {

		LOGGER.info("Storing XML data and extracting document information ...");

		for (Noark53PackageEntity entity : structure.values()) {

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
						xmlHandler.getValidationGroup().getNextGroupId(), "Parse errors",
						"Exceptions that occurred while parsing the package XML files. Such exceptions might "
								+ "indicate an error in the validator itself and should be reported to its "
								+ "developers. Test results cannot be trusted upon such errors.",
						xmlHandler.getValidationGroup().getName());
				errorResult.addErrors(exceptionHandler.getExceptionsAsItems());
				ValidationCollector.get().collect(errorResult);
			}
		}
	}

	/**
	 * Retrieves the checksums of all entities in the extraction package and stores them in the addml.property {@link
	 * ItemDef}.
	 */
	private void storePackageChecksums() throws Exception {

		for (Noark53PackageEntity entity : structure.values()) {

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

		String validationFileLocation = Noark53Command.COMMAND_NAME + "/noark53-validation.xml";

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

			ValidationCollector.get().collect(result);
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

			ValidationCollector.get().collect(result);
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
