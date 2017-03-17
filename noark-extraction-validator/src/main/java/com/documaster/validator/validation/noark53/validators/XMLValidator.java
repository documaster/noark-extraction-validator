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
package com.documaster.validator.validation.noark53.validators;

import java.io.File;
import java.util.List;

import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationResult;
import com.documaster.validator.validation.noark53.model.Noark53PackageEntity;
import com.documaster.validator.validation.noark53.provider.ValidationGroup;
import com.documaster.validator.validation.utils.DefaultXMLHandler;
import com.documaster.validator.validation.utils.SchemaValidator;
import com.documaster.validator.validation.utils.WellFormedXmlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLValidator.class);

	public static boolean isValid(Noark53PackageEntity entity, boolean ignoreNonComplianceToSchema) {

		LOGGER.info("Validating XML File {} ...", entity.getXmlFile());

		ValidationResult result = new ValidationResult(
				ValidationGroup.PACKAGE.getNextGroupId(), entity.getXmlFileName() + " integrity",
				"Tests whether the XML file 1) exists, 2) is valid XML, 3) complies with the Noark schemas, "
						+ "and, optionally, 4) complies with the custom schemas",
				ValidationGroup.PACKAGE.getName());

		boolean exists = validateExistence(entity.getXmlFile(), result);
		boolean isWellFormed = validateIntegrity(entity.getXmlFile(), result);

		// Package schemas
		validateAgainstSchemas(
				entity.getXmlFile(), result, entity.getPackageSchemas(), NoarkXMLHandler.Schema.PACKAGE, false);

		// Noark schemas
		boolean compliesWithNoarkSchemas = validateAgainstSchemas(
				entity.getXmlFile(), result, entity.getNoarkSchemas(), NoarkXMLHandler.Schema.NOARK, true);

		// Custom schemas
		if (entity.hasCustomSchemas()) {
			validateAgainstSchemas(
					entity.getXmlFile(), result, entity.getCustomSchemas(), NoarkXMLHandler.Schema.CUSTOM, true);
		}

		ValidationCollector.get().collect(result);

		return exists && isWellFormed && (compliesWithNoarkSchemas || ignoreNonComplianceToSchema);
	}

	private static boolean validateExistence(File xmlFile, ValidationResult result) {

		if (!xmlFile.isFile()) {
			result.addError(new BaseItem().add("Error", "Missing file: " + xmlFile.getName()));
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xmlFile.getName() + " exists"));
			return true;
		}
	}

	private static boolean validateIntegrity(File xmlFile, ValidationResult result) {

		WellFormedXmlValidator xmlIntegrityValidator = new WellFormedXmlValidator<>(new DefaultXMLHandler());

		if (!xmlFile.isFile() || !xmlIntegrityValidator.isXmlWellFormed(xmlFile)) {
			result.addErrors(xmlIntegrityValidator.getExceptionHandler().getExceptionsAsItems());
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xmlFile.getName() + " is well-formed"));
			return true;
		}
	}

	private static boolean validateAgainstSchemas(
			File xmlFile, ValidationResult result, List<File> xsdSchemas, NoarkXMLHandler.Schema schemaType,
			boolean isError) {

		NoarkXMLHandler handler = new NoarkXMLHandler(schemaType);
		SchemaValidator schemaValidator = new SchemaValidator<>(handler);

		if (!xmlFile.isFile() || !schemaValidator.isXmlFileValid(xmlFile, xsdSchemas)) {

			// Report errors/warnings
			List<BaseItem> exceptionsAsItems = handler.getExceptionsAsItems();
			if (isError) {
				result.addErrors(exceptionsAsItems);
			} else {
				result.addWarnings(exceptionsAsItems);
			}

			// Report a summary of the errors and warnings
			List<BaseItem> summaryItems = handler.getSummaryOfExceptionsAsItems();
			result.addSummaries(summaryItems);
			result.addInformation(new BaseItem().add("Information",
					String.format("Distinct errors in %s schema(s): %d", schemaType, summaryItems.size())));

			return false;
		}

		result.addInformation(new BaseItem()
				.add("Information", String.format("%s validates against %s schema(s)", xmlFile.getName(), schemaType)));

		return true;
	}
}
