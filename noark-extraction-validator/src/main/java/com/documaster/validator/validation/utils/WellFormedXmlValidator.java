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
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

public class WellFormedXmlValidator<T extends AbstractReusableXMLHandler> {

	private static final Logger LOGGER = LoggerFactory.getLogger(WellFormedXmlValidator.class);

	private T handler;

	public WellFormedXmlValidator(T handler) {

		Validate.notNull(handler);

		this.handler = handler;
	}

	public T getExceptionHandler() {

		return handler;
	}

	public boolean isXmlWellFormed(File xmlFile) {

		handler.reset();

		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setNamespaceAware(true);

		try (
				FileInputStream fis = new FileInputStream(xmlFile);
				BufferedInputStream bis = new BufferedInputStream(fis)) {

			SAXParser parser = saxParserFactory.newSAXParser();
			parser.parse(bis, handler);

		} catch (Exception ex) {
			LOGGER.error("Well-formed XML validation failed with exception: ", ex);
			handler.fatalError(new SAXParseException(ex.getMessage(), null));
		}

		return !handler.hasExceptions();
	}
}
