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
package com.documaster.validator.validation.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

public class WellFormedXmlValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(WellFormedXmlValidator.class);

	private ValidationErrorHandler validationErrorHandler;

	private Set<String> errors = new HashSet<>();

	public Set<String> getErrors() {

		return errors;
	}

	private void buildErrorsSet(String parseError) {

		errors.clear();
		if (!StringUtils.isBlank(parseError)) {
			errors.add(parseError);
		}
		errors.addAll(getValidationErrors());
	}

	public boolean isXmlWellFormed(File xmlFile) {

		SAXParserFactory factory = SAXParserFactory.newInstance();

		factory.setNamespaceAware(true);

		String parseError = null;
		try (
				FileInputStream fis = new FileInputStream(xmlFile);
				BufferedInputStream bis = new BufferedInputStream(fis)) {

			SAXParser parser = factory.newSAXParser();

			validationErrorHandler = new ValidationErrorHandler();

			XMLReader reader = parser.getXMLReader();
			reader.setErrorHandler(validationErrorHandler);
			reader.parse(new InputSource(bis));

		} catch (Exception ex) {
			parseError = "Could not parse XML File " + xmlFile;
			LOGGER.warn(parseError, ex);
		}

		buildErrorsSet(parseError);

		return getErrors().size() == 0;
	}

	private Set<String> getValidationErrors() {

		if (validationErrorHandler == null) {
			return Collections.emptySet();
		}

		Set<String> validationErrors = new HashSet<>();

		for (SAXParseException ex : validationErrorHandler.getExceptions()) {
			validationErrors.add(MessageFormat
					.format("Line {0}: Column {1}: {2}", ex.getLineNumber(), ex.getColumnNumber(), ex.getMessage()));
		}

		return validationErrors;
	}
}
