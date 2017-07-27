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
import com.documaster.validator.validation.collector.ValidationResult;
import com.documaster.validator.validation.noark53.provider.ValidationGroup;
import com.documaster.validator.validation.utils.ChecksumCalculator;
import com.documaster.validator.validation.utils.DefaultXMLHandler;
import com.documaster.validator.validation.utils.WellFormedXmlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XSDValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(XSDValidator.class);

	private ValidationCollector collector;

	public XSDValidator(ValidationCollector collector) {

		this.collector = collector;
	}

	public boolean isValid(File xsdFile, String checksum) {

		LOGGER.info("Validating XSD File {} ...", xsdFile);

		ValidationResult result = new ValidationResult(
				ValidationGroup.PACKAGE.getNextGroupId(collector), xsdFile.getName() + " integrity",
				"Tests whether the XSD schema 1) exists, 2) is valid XML, and 3) its checksum "
						+ "matches the checksum of its Noark counterpart",
				ValidationGroup.PACKAGE.getName());

		boolean exists = validateExistence(xsdFile, result);
		boolean isWellFormed = validateIntegrity(xsdFile, result);
		boolean checksumMatches = validateChecksum(xsdFile, result, checksum);

		collector.collect(result);

		return exists && isWellFormed && checksumMatches;
	}

	private static boolean validateExistence(File xsdFile, ValidationResult result) {

		if (!xsdFile.isFile()) {
			result.addError(new BaseItem().add("Error", "Missing file: " + xsdFile.getName()));
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xsdFile.getName() + " exists"));
			return true;
		}
	}

	private static boolean validateIntegrity(File xsdFile, ValidationResult result) {

		WellFormedXmlValidator xmlIntegrityValidator = new WellFormedXmlValidator<>(new DefaultXMLHandler());

		if (!xsdFile.isFile() || !xmlIntegrityValidator.isXmlWellFormed(xsdFile)) {
			result.addErrors(xmlIntegrityValidator.getExceptionHandler().getExceptionsAsItems());
			return false;
		} else {
			result.addInformation(new BaseItem().add("Information", xsdFile.getName() + " is well-formed"));
			return true;
		}
	}

	private static boolean validateChecksum(File xsdFile, ValidationResult result, String checksum) {

		if (!xsdFile.isFile() || !checksum.equalsIgnoreCase(ChecksumCalculator.getFileSha256Checksum(xsdFile))) {
			result.addWarning(
					new BaseItem().add(
							"Warning",
							"Checksum does not match the checksum of the XSD schema distributed with Noark 5"));
			return false;
		} else {
			result.addInformation(
					new BaseItem().add(
							"Information",
							"Checksum matches the checksum of the XSD schema distributed with Noark 5"));
			return true;
		}
	}
}
