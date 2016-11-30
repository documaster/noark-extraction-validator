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
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.documaster.validator.config.commands.Noark53Command;
import com.documaster.validator.converters.xsd.XsdConverter;
import com.documaster.validator.exceptions.ValidationException;
import com.documaster.validator.reporting.core.ReporterFactory;
import com.documaster.validator.storage.core.Storage;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.storage.model.Field;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.validation.Validator;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationCollector.ValidationResult;
import com.documaster.validator.validation.noark53.parsers.BaseHandler;
import com.documaster.validator.validation.noark53.parsers.HandlerFactory;
import com.documaster.validator.validation.noark53.provider.ValidationData;
import com.documaster.validator.validation.noark53.provider.ValidationGroup;
import com.documaster.validator.validation.noark53.provider.ValidationProvider;
import com.documaster.validator.validation.noark53.provider.ValidationRule;
import com.documaster.validator.validation.noark53.validators.XMLValidator;
import com.documaster.validator.validation.noark53.validators.XSDValidator;
import com.documaster.validator.validation.utils.ChecksumCalculator;
import com.documaster.validator.validation.utils.ValidationErrorHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class Noark53Validator extends Validator<Noark53Command> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Noark53Validator.class);

	private Map<String, File> tempXsdSchemas;

	private XsdConverter converter;

	public Noark53Validator(Noark53Command command) {

		super(command);
	}

	@Override
	public void run() throws Exception {

		LOGGER.info("Executing Noark 5.3 extraction validation ...");

		try {
			// Validate the extraction package structure
			createTempSchemas();
			validateArchiveStructure();

			// Init storage
			Storage.init(getCommand().getStorageDelegate(), getCommand().getProperties().getUniqueFieldsMap());
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

			ValidationResult error = new ValidationResult("Exceptions", ValidationGroup.COMMON);
			error.addError(new BaseItem().add("exception", ex.getMessage()));

			ValidationCollector.get().collect(error);

		} finally {

			ReporterFactory.createReporter(getCommand().getReporterDelegate(), getArchiveTitle()).createReport();

			if (Storage.get() != null) {
				Storage.get().stopWriter();
				Storage.get().destroy();
			}

			cleanUpTempSchemas();
		}
	}

	/**
	 * Creates temporary files from the archive schemas distributed with the tool.
	 */
	private void createTempSchemas() throws Exception {

		tempXsdSchemas = new HashMap<>();

		for (String xsdFileName : getCommand().getProperties().getPackageStructure().getAllXSDFiles()) {

			String xsdFileLocation = Noark53Command.COMMAND_NAME + "/" + xsdFileName;

			String tempDir = System.getProperty("java.io.tmpdir");

			File tempXsdFile = new File(tempDir, xsdFileName);

			FileUtils.copyInputStreamToFile(
					getClass().getClassLoader().getResourceAsStream(xsdFileLocation),
					tempXsdFile);

			tempXsdSchemas.put(xsdFileName, tempXsdFile);
		}
	}

	/**
	 * Validates the integrity of the archive structure.
	 */
	private void validateArchiveStructure() throws Exception {

		// Validate the schemas in the extraction package
		for (String xsdSchemaName : getCommand().getProperties().getPackageStructure().getAllXSDFiles()) {
			File xsdSchema = new File(getCommand().getExtractionDirectory(), xsdSchemaName);
			XSDValidator.validate(xsdSchema, getCommand().getProperties().getChecksumFor(xsdSchemaName));
		}

		boolean stopValidation = false;

		// Validate the XML files in the package against the bundled XSD schemas
		for (String xmlFileName : getCommand().getProperties().getPackageStructure().getAllXMLFiles()) {

			File xmlFile = new File(getCommand().getExtractionDirectory(), xmlFileName);
			Set<String> xsdSchemaNames = getCommand().getProperties().getPackageStructure()
					.getXSDSchemasFor(xmlFileName);

			List<File> xsdSchemaFiles = new ArrayList<>();
			for (String xsdSchemaName : xsdSchemaNames) {
				xsdSchemaFiles.add(tempXsdSchemas.get(xsdSchemaName));
			}

			boolean isXMLValid = XMLValidator
					.validate(xmlFile, xsdSchemaFiles, getCommand().getIgnoreNonCompliantXML());
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
		converter.convert(new ArrayList<>(tempXsdSchemas.values()));

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

		for (String xmlFilename : getCommand().getProperties().getPackageStructure().getAllXMLFiles()) {

			File xmlFile = new File(getCommand().getExtractionDirectory(), xmlFilename);

			ValidationErrorHandler errorHandler = new ValidationErrorHandler();
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();

			XMLReader reader = saxParser.getXMLReader();

			BaseHandler xmlHandler = HandlerFactory.createHandler(xmlFile, reader, converter.getItemDefs());

			reader.setContentHandler(xmlHandler);
			reader.setErrorHandler(errorHandler);

			try (
					FileInputStream fis = new FileInputStream(xmlFile);
					BufferedInputStream bis = new BufferedInputStream(fis)) {
				reader.parse(new InputSource(bis));
			}

			for (SAXException ex : errorHandler.getExceptions()) {

				ValidationCollector.get().collect(
						new ValidationResult(ex.getMessage(), xmlHandler.getValidationGroup()));
			}
		}
	}

	/**
	 * Retrieves the checksums of all documents in the extraction package and stores them in the addml.property {@link
	 * ItemDef}.
	 */
	private void storePackageChecksums() throws Exception {

		// Store the checksums of all XSD files
		for (String xsdSchemaName : getCommand().getProperties().getPackageStructure().getAllXSDFiles()) {

			File xsdSchema = new File(getCommand().getExtractionDirectory(), xsdSchemaName);

			Item xsdSchemaChecksumItem = new Item(converter.getItemDefs().get("addml.property"));
			xsdSchemaChecksumItem.add("name", xsdSchemaName);
			xsdSchemaChecksumItem.add("value", ChecksumCalculator.getFileSha256Checksum(xsdSchema));

			Storage.get().write(xsdSchemaChecksumItem);
		}

		// Store the checksums of all XML files
		for (String xmlFileName : getCommand().getProperties().getPackageStructure().getAllXMLFiles()) {

			File xmlFile = new File(getCommand().getExtractionDirectory(), xmlFileName);

			Item xmlFileChecksumItem = new Item(converter.getItemDefs().get("addml.property"));
			xmlFileChecksumItem.add("name", xmlFileName);
			xmlFileChecksumItem.add("value", ChecksumCalculator.getFileSha256Checksum(xmlFile));

			Storage.get().write(xmlFileChecksumItem);
		}
	}

	private void runValidationQueries() throws Exception {

		LOGGER.info("Validating extraction ...");

		String validationFileLocation = Noark53Command.COMMAND_NAME + "/noark53-validation.xml";

		JAXBContext jaxbContext = JAXBContext.newInstance(
				ValidationProvider.class, ValidationData.class, ValidationGroup.class, ValidationRule.class);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

		ValidationProvider vp;
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(validationFileLocation)) {
			vp = (ValidationProvider) jaxbUnmarshaller.unmarshal(is);
		}

		for (ValidationRule rule : vp.getRules()) {

			LOGGER.info(MessageFormat.format("Validating {0} ...", rule.getTitle()));

			String informationRequest = rule.getData().getInformationRequest();
			String errorRequest = rule.getData().getErrorsRequest();

			ValidationResult result = new ValidationResult(rule.getTitle(), rule.getGroup());

			if (informationRequest != null && !StringUtils.isBlank(informationRequest)) {
				result.addInformation(Storage.get().fetch(informationRequest));
			}

			if (errorRequest != null && !StringUtils.isBlank(errorRequest)) {
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

	private void cleanUpTempSchemas() {

		if (tempXsdSchemas != null) {
			for (File xsdSchema : tempXsdSchemas.values()) {
				FileUtils.deleteQuietly(xsdSchema);
			}
		}
	}
}
