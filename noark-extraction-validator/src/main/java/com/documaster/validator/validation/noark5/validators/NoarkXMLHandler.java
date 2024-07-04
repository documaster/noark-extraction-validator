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
package com.documaster.validator.validation.noark5.validators;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.documaster.validator.exceptions.aggregation.SAXParseExceptionAggregator;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.validation.utils.AbstractReusableXMLHandler;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * An {@link AbstractReusableXMLHandler} implementation that captures the {@link SAXParseException}s raised during
 * parsing, enriches them with element metadata information, and transforms them to {@link BaseItem}s.
 */
class NoarkXMLHandler extends AbstractReusableXMLHandler {

	private Schema schema;
	private String elementType;
	private final Map<NoarkReferenceField, String> idFields = new LinkedHashMap<>();
	private List<NoarkSAXParseException> exceptions = new LinkedList<>();
	private StringBuilder characters = new StringBuilder();

	NoarkXMLHandler(Schema schema) {

		this.schema = schema;
	}

	private void setElementType(String qName, Attributes attributes) {

		if (!NoarkElementType.contains(qName)) {
			return;
		}

		elementType = null;
		for (int i = 0; i < attributes.getLength(); i++) {
			if (attributes.getQName(i).endsWith("type")) {
				elementType = attributes.getValue(i);
				break;
			}
		}

		elementType = !StringUtils.isBlank(elementType) ? elementType : qName;
	}

	private void setFieldValue(NoarkReferenceField field, String value) {

		idFields.put(field, StringUtils.trimToNull(value));
	}

	@Override
	public boolean hasExceptions() {

		return !exceptions.isEmpty();
	}

	@Override
	public List<BaseItem> getSummaryOfExceptionsAsItems() {

		return new SAXParseExceptionAggregator<NoarkSAXParseException>()
				.aggregate(exceptions)
				.entrySet()
				.stream()
				.map(k -> new BaseItem()
						.add("Schema", schema.toString())
						.add("Message", k.getKey())
						.add("Count", k.getValue()))
				.collect(collectingAndThen(toList(), Collections::unmodifiableList));
	}

	@Override
	public List<BaseItem> getExceptionsAsItems() {

		return exceptions.stream()
				.sorted(new SAXParseExceptionComparator())
				.map(ex -> {

					BaseItem item = new BaseItem();

					item.add("Schema", schema.toString());
					item.add("Type", ex.getElementType());
					ex.getIdFields().forEach((k, v) -> item.add(k.toString(), v));
					item.add("Line", ex.getLineNumber() != -1 ? ex.getLineNumber() : null);
					item.add("Column", ex.getColumnNumber() != -1 ? ex.getColumnNumber() : null);
					item.add("Message", ex.getMessage());

					return item;
				}).collect(collectingAndThen(toList(), Collections::unmodifiableList));
	}

	@Override
	public void reset() {

		exceptions.clear();
		characters.setLength(0);
		elementType = null;
		Stream.of(NoarkReferenceField.values()).forEach(v -> idFields.put(v, null));
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) {

		characters.setLength(0);
		setElementType(qName, attributes);
	}

	@Override
	public void characters(char buf[], int offset, int length) {

		characters.append(new String(buf, offset, length));
	}

	@Override
	public void endElement(String uri, String localName, String qName) {

		for (NoarkReferenceField field : NoarkReferenceField.values()) {
			if (qName.equalsIgnoreCase(field.getName())) {
				setFieldValue(field, characters.toString().trim());
				break;
			}
		}
	}

	@Override
	public void warning(SAXParseException exception) {

		exceptions.add(new NoarkSAXParseException(exception, elementType, idFields));
	}

	@Override
	public void error(SAXParseException exception) {

		exceptions.add(new NoarkSAXParseException(exception, elementType, idFields));
	}

	@Override
	public void fatalError(SAXParseException exception) {

		exceptions.add(new NoarkSAXParseException(exception, elementType, idFields));
	}

	private static class NoarkSAXParseException extends SAXParseException {

		private Map<NoarkReferenceField, String> idFields;
		private String elementType;

		NoarkSAXParseException(SAXParseException ex, String elementType, Map<NoarkReferenceField, String> idFields) {

			super(ex.getMessage(), ex.getPublicId(), ex.getSystemId(), ex.getLineNumber(), ex.getColumnNumber());
			this.elementType = elementType;
			this.idFields = idFields;
		}

		String getElementType() {

			return elementType;
		}

		Map<NoarkReferenceField, String> getIdFields() {

			return idFields;
		}
	}

	public enum Schema {
		PACKAGE, NOARK, CUSTOM;
	}

	private enum NoarkElementType {

		ARKIV, ARKIVSKAPER, ARKIVDEL, KLASSIFIKASJONSSYSTEM, KLASSE, SAKSMAPPE, MAPPE, JOURNALPOST, MOETEMAPPE,
		REGISTRERING, SAKSPART, BASISREGISTRERING, MOETEDELTAKER, KORRESPONDANSEPART, AVSKRIVNING, DOKUMENTFLYT,
		DOKUMENTBESKRIVELSE, MOETEREGISTRERING, DOKUMENTOBJEKT, KONVERTERING, KRYSSREFERANSE, MERKNAD, KASSASJON,
		UTFOERTKASSASJON, SLETTING, SKJERMING, GRADERING, PRESEDENS, ELEKTRONISKSIGNATUR, ENDRING, JOURNALHODE,
		JOURNALREGISTRERING, ENDRINGSLOGG, OFFENTLIGJOURNAL, LOEPENDEJOURNAL;

		public static boolean contains(String value) {

			if (StringUtils.isBlank(value)) {
				return false;
			}
			try {
				Enum.valueOf(NoarkElementType.class, value.toUpperCase());
				return true;
			} catch (IllegalArgumentException | NullPointerException ex) {
				return false;
			}
		}
	}

	private enum NoarkReferenceField {

		FILE_ID("mappeid"),
		CASE_YEAR("saksaar"),
		CASE_NUMBER("sakssekvensnummer"),
		RECORD_ID("registreringsid"),
		RECORD_YEAR("journalaar"),
		RECORD_NUMBER("journalsekvensnummer"),
		SYSTEM_ID("systemid");

		private String name;

		NoarkReferenceField(String name) {

			this.name = name;
		}

		public String getName() {

			return name;
		}
	}
}
