/**
 * Noark Extraction Validator
 * Copyright (C) 2017, Documaster AS
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

import java.util.Comparator;
import java.util.List;

import com.documaster.validator.storage.model.BaseItem;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A reusable {@link DefaultHandler} implementation that adds a contract for retrieving the encountered exceptions as a
 * sorted list of {@link BaseItem}s.
 */
public abstract class AbstractReusableXMLHandler extends DefaultHandler {

	/**
	 * Indicates whether the handler encountered any exceptions during parsing.
	 */
	public abstract boolean hasExceptions();

	/**
	 * Returns a summary of the encountered exceptions as {@link BaseItem}s.
	 */
	public abstract List<BaseItem> getSummaryOfExceptionsAsItems();

	/**
	 * Sorts the encountered exceptions and returns an ordered list of {@link BaseItem}s created from the sorted list of
	 * exceptions.
	 * <br/>
	 * A default {@link SAXParseExceptionComparator} implementation is provided as part of this abstract class.
	 * <br/>
	 * See the referenced {@link DefaultHandler} methods listed below for more information on retrieving parsing errors.
	 *
	 * @see #warning(SAXParseException)
	 * @see #error(SAXParseException)
	 * @see #fatalError(SAXParseException)
	 */
	public abstract List<BaseItem> getExceptionsAsItems();

	/**
	 * Provides logic for resetting the instance so that it can be safely reused.
	 */
	public abstract void reset();

	@Override
	public void startDocument() {

		reset();
	}

	@Override
	public void warning(SAXParseException exception) {

		// Ignore in the abstract implementation
		// Serves the purpose to remove the throws declaration
	}

	@Override
	public void error(SAXParseException exception) {

		// Ignore in the abstract implementation
		// Serves the purpose to remove the throws declaration
	}

	@Override
	public void fatalError(SAXParseException exception) {

		// Ignore in the abstract implementation
		// Serves the purpose to remove the throws declaration
	}

	/**
	 * A default {@link SAXParseException} comparator implementation that takes the following precedence:
	 * <ol>
	 * <li>message</li>
	 * <li>line number</li>
	 * <li>column number</li>
	 * </ol>
	 */
	public static class SAXParseExceptionComparator implements Comparator<SAXParseException> {

		@Override
		public int compare(SAXParseException o1, SAXParseException o2) {

			int messageComparisonResult = o1.getMessage().compareTo(o2.getMessage());
			if (messageComparisonResult != 0) {
				return messageComparisonResult;
			}

			int lineComparisonResult = Integer.compare(o1.getLineNumber(), o2.getLineNumber());
			if (lineComparisonResult != 0) {
				return lineComparisonResult;
			}

			return Integer.compare(o1.getColumnNumber(), o2.getColumnNumber());
		}
	}
}
