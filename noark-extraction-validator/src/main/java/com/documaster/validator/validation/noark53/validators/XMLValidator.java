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
import com.documaster.validator.validation.noark53.provider.ValidationGroup;
import com.documaster.validator.validation.noark53.model.Noark53PackageEntity;
import com.documaster.validator.validation.utils.SchemaValidator;
import com.documaster.validator.validation.utils.WellFormedXmlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLValidator.class);

	public static boolean isValid(Noark53PackageEntity entity, boolean ignoreNonComplianceToSchema) {

		LOGGER.info("Validating XML File {} ...", entity.getXmlFile());

		ValidationCollector.ValidationResult result = new ValidationCollector.ValidationResult(
				entity.getXmlFileName() + " integrity", ValidationGroup.COMMON);

		boolean exists = validateExistence(entity.getXmlFile(), result);
		boolean isWellFormed = validateIntegrity(entity.getXmlFile(), result);
		validateAgainstPackageSchemas(entity.getXmlFile(), result, entity.getPackageSchemas());
		boolean compliesWithNoarkSchemas = validateAgainstNoarkSchemas(
				entity.getXmlFile(), result, entity.getNoarkSchemas());

		ValidationCollector.get().collect(result);

		return exists && isWellFormed && (compliesWithNoarkSchemas || ignoreNonComplianceToSchema);
	}

	private static boolean validateExistence(File xmlFile, ValidationCollector.ValidationResult result) {

		if (!xmlFile.isFile()) {
			result.addError(new BaseItem().add("Error", "Missing file: " + xmlFile.getName()));
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xmlFile.getName() + " exists"));
			return true;
		}
	}

	private static boolean validateIntegrity(File xmlFile, ValidationCollector.ValidationResult result) {

		WellFormedXmlValidator xmlIntegrityValidator = new WellFormedXmlValidator();

		if (!xmlFile.isFile() || !xmlIntegrityValidator.isXmlWellFormed(xmlFile)) {
			for (String error : xmlIntegrityValidator.getErrors()) {
				result.addError(new BaseItem().add("Error", error));
			}
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xmlFile.getName() + " is well-formed"));
			return true;
		}
	}

	private static boolean validateAgainstPackageSchemas(
			File xmlFile, ValidationCollector.ValidationResult result, List<File> xsdSchemas) {

		SchemaValidator schemaValidator = new SchemaValidator();

		if (!xmlFile.isFile() || !schemaValidator.isXmlFileValid(xmlFile, xsdSchemas)) {
			for (String error : schemaValidator.getErrors()) {
				result.addWarning(new BaseItem().add("Warnings", "Package schema: " + 	error));
			}
			return false;
		} else {
			result.addInformation(
					new BaseItem().add("Information", xmlFile.getName() + " validates against package schemas"));
			return true;
		}
	}

	private static boolean validateAgainstNoarkSchemas(
			File xmlFile, ValidationCollector.ValidationResult result, List<File> xsdSchemas) {

		SchemaValidator schemaValidator = new SchemaValidator();

		if (!xmlFile.isFile() || !schemaValidator.isXmlFileValid(xmlFile, xsdSchemas)) {
			for (String error : schemaValidator.getErrors()) {
				result.addError(new BaseItem().add("Errors", "Noark schema: " + error));
			}
			return false;
		} else {
			result.addInformation(
					new BaseItem().add("Information", xmlFile.getName() + " validates against Noark schemas"));
			return true;
		}
	}
}
