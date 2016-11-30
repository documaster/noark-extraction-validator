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
package com.documaster.validator.converters.xsd;

import com.documaster.validator.converters.Converter;
import com.sun.tools.xjc.AbortException;
import com.sun.tools.xjc.api.ErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

class ConversionErrorListener implements ErrorListener {

	private Logger logger;

	ConversionErrorListener(Class<? extends Converter> reportingClass) {

		logger = LoggerFactory.getLogger(reportingClass);
	}

	@Override
	public void error(SAXParseException exception) throws AbortException {

		logError(exception);
	}

	@Override
	public void fatalError(SAXParseException exception) throws AbortException {

		logError(exception);
	}

	@Override
	public void warning(SAXParseException exception) throws AbortException {

		logError(exception);
	}

	@Override
	public void info(SAXParseException exception) {

		logInfo(exception);
	}

	private void logError(SAXParseException exception) {

		logger.error(exception.getMessage(), exception);
	}

	private void logInfo(SAXParseException exception) {

		logger.info(exception.getMessage());
	}
}
