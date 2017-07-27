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
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

public class SchemaValidator<T extends AbstractReusableXMLHandler> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidator.class);

	private T handler;

	/**
	 * Creates a new schema validator using the specified {@link AbstractReusableXMLHandler} implementation.
	 * <p/>
	 * Callers which do not require special handling of the content or the raised exceptions can instantiate the class
	 * with a new {@link DefaultXMLHandler} instance.
	 *
	 * @param handler
	 * 		The XML content and exception handler to be used during the parsing.
	 */
	public SchemaValidator(T handler) {

		this.handler = handler;
	}

	public T getHandler() {

		return handler;
	}

	public boolean isXmlFileValid(File xmlFile, List<File> xsdFiles) {

		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setValidating(false);
		saxParserFactory.setNamespaceAware(true);

		try {
			StreamSource[] sources = xsdFiles.stream().map(StreamSource::new).toArray(StreamSource[]::new);

			Schema schema = schemaFactory.newSchema(sources);
			saxParserFactory.setSchema(schema);

			SAXParser parser = saxParserFactory.newSAXParser();
			parser.parse(xmlFile, handler);

		} catch (Exception ex) {
			LOGGER.error("Schema validation failed with exception: ", ex);
			handler.fatalError(new SAXParseException(ex.getMessage(), null));
		}

		return !handler.hasExceptions();
	}
}
