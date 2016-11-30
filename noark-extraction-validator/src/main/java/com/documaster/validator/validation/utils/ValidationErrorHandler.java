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

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ValidationErrorHandler implements ErrorHandler {

	private List<SAXParseException> exceptions = new ArrayList<>();

	public List<SAXParseException> getExceptions() {

		return exceptions;
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {

		exceptions.add(exception);
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {

		exceptions.add(exception);
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {

		exceptions.add(exception);
	}
}
