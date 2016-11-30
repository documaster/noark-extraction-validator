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

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

public class SchemaValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidator.class);

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

	public boolean isXmlFileValid(File xmlFile, List<File> xsdFiles) {

		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		StreamSource[] schemasStreamSource = new StreamSource[xsdFiles.size()];

		for (int i = 0; i < schemasStreamSource.length; i++) {
			schemasStreamSource[i] = new StreamSource(xsdFiles.get(i));
		}

		String parseError = null;
		try {
			Schema schema = factory.newSchema(schemasStreamSource);

			validationErrorHandler = new ValidationErrorHandler();

			Validator validator = schema.newValidator();
			validator.setErrorHandler(validationErrorHandler);
			validator.validate(new StreamSource(xmlFile));

		} catch (Exception ex) {
			parseError = "Schema validation could not be performed for " + xmlFile;
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
