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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.documaster.validator.exceptions.aggregation.SAXParseExceptionAggregator;
import com.documaster.validator.storage.model.BaseItem;
import org.xml.sax.SAXParseException;

/**
 * An {@link AbstractReusableXMLHandler} implementation that transforms the raised {@link SAXParseException} during
 * parsing to {@link BaseItem}s.
 */
public class DefaultXMLHandler extends AbstractReusableXMLHandler {

	private List<SAXParseException> exceptions = new LinkedList<>();

	public boolean hasExceptions() {

		return !exceptions.isEmpty();
	}

	@Override
	public List<BaseItem> getSummaryOfExceptionsAsItems() {

		return new SAXParseExceptionAggregator<>()
				.aggregate(exceptions)
				.entrySet()
				.stream()
				.map(k -> new BaseItem()
						.add("Message", k.getKey())
						.add("Count", k.getValue()))
				.collect(collectingAndThen(toList(), Collections::unmodifiableList));
	}

	public List<BaseItem> getExceptionsAsItems() {

		return exceptions.stream().sorted(new SAXParseExceptionComparator())
				.map(ex -> new BaseItem()
						.add("Line", ex.getLineNumber() != -1 ? ex.getLineNumber() : null)
						.add("Column", ex.getColumnNumber() != -1 ? ex.getColumnNumber() : null)
						.add("Message", ex.getMessage()))
				.collect(collectingAndThen(toList(), Collections::unmodifiableList));
	}

	public void reset() {

		exceptions.clear();
	}

	@Override
	public void warning(SAXParseException exception) {

		exceptions.add(exception);
	}

	@Override
	public void error(SAXParseException exception) {

		exceptions.add(exception);
	}

	@Override
	public void fatalError(SAXParseException exception) {

		exceptions.add(exception);
	}
}
