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

import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.noark53.provider.ValidationGroup;
import com.documaster.validator.validation.utils.ChecksumCalculator;
import com.documaster.validator.validation.utils.WellFormedXmlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XSDValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(XSDValidator.class);

	public static boolean validate(File xsdFile, String checksum) {

		LOGGER.info("Validating XML File {} ...", xsdFile);

		ValidationCollector.ValidationResult result = new ValidationCollector.ValidationResult(
				xsdFile.getName() + " integrity", ValidationGroup.COMMON);

		boolean exists = validateExistence(xsdFile, result);
		boolean isWellFormed = validateIntegrity(xsdFile, result);
		boolean checksumMatches = validateChecksum(xsdFile, result, checksum);

		ValidationCollector.get().collect(result);

		return exists && isWellFormed && checksumMatches;
	}

	private static boolean validateExistence(File xsdFile, ValidationCollector.ValidationResult result) {

		if (!xsdFile.isFile()) {
			result.addError(new BaseItem().add("Error", "Missing file: " + xsdFile.getName()));
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xsdFile.getName() + " exists"));
			return true;
		}
	}

	private static boolean validateIntegrity(File xsdFile, ValidationCollector.ValidationResult result) {

		WellFormedXmlValidator xmlIntegrityValidator = new WellFormedXmlValidator();

		if (!xsdFile.isFile() || !xmlIntegrityValidator.isXmlWellFormed(xsdFile)) {
			for (String error : xmlIntegrityValidator.getErrors()) {
				result.addError(new BaseItem().add("Error", error));
			}
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xsdFile.getName() + " is well-formed"));
			return true;
		}
	}

	private static boolean validateChecksum(
			File xsdFile, ValidationCollector.ValidationResult result, String checksum) {

		if (!xsdFile.isFile() || !checksum.equalsIgnoreCase(ChecksumCalculator.getFileSha256Checksum(xsdFile))) {
			result.addError(new BaseItem().add("Error", "Checksum does not match the one distributed with Noark 5"));
			return false;
		} else {
			result.addInformation(
					new BaseItem().add("Information", "Checksum matches the one distributed with Noark 5"));
			return true;
		}
	}
}
