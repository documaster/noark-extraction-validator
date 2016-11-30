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
package com.documaster.validator.validation.noark53.parsers;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.documaster.validator.storage.core.Storage;
import com.documaster.validator.storage.model.Field;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.validation.noark53.provider.ValidationGroup;
import com.documaster.validator.validation.utils.ChecksumCalculator;
import com.documaster.validator.validation.utils.PDFAValidator;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A handler for Noark 5 arkivstruktur.xml file.
 */
class ArchiveStructureHandler extends BaseHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStructureHandler.class);

	private boolean isInLeafElementWithTheSameName = false;
	private boolean isInBusinessSpecificMetadata = false;

	ArchiveStructureHandler(File xmlFile, XMLReader reader, Map<String, ItemDef> itemDefs) {

		super(xmlFile, reader, itemDefs, ValidationGroup.ARCHIVE_STRUCTURE);
	}

	private ArchiveStructureHandler(ArchiveStructureHandler parentHandler, ItemDef itemDefs) {

		super(parentHandler, itemDefs, ValidationGroup.ARCHIVE_STRUCTURE);
	}

	@Override
	public void startElement(String uri, String local, String qName, Attributes attributes) throws SAXException {

		characters.setLength(0);

		// Ignore business-specific metadata
		if (isInBusinessSpecificMetadata || qName.equalsIgnoreCase("virksomhetsspesifikkeMetadata")) {
			isInBusinessSpecificMetadata = true;
			return;
		}

		String nextElementName = qName.toLowerCase();
		String nextItemDefName = getItemDefNameForElement(nextElementName);

		if (getItemDefs().containsKey(nextItemDefName)) {
			if (getItem() != null) {
				String currentItemDefName = getItem().getItemDef().getFullName();
				boolean hasLeafElementWithTheSameName = getItem().getItemDef().hasFieldWithName(nextElementName);

				if (currentItemDefName.equals(nextItemDefName) && hasLeafElementWithTheSameName) {
					isInLeafElementWithTheSameName = true;
					return;
				}
			}

			// Fetch type from *type attribute
			String type = null;
			for (int i = 0; i < attributes.getLength(); i++) {
				if (attributes.getQName(i).endsWith("type")) {
					type = attributes.getValue(i);
					break;
				}
			}

			setContentHandler(new ArchiveStructureHandler(this, getItemDefs().get(nextItemDefName)), type);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		// Ignore business-specific metadata
		if (isInBusinessSpecificMetadata) {
			if (qName.equalsIgnoreCase("virksomhetsspesifikkeMetadata")) {
				isInBusinessSpecificMetadata = false;
			}
			return;
		}

		String elementName = qName.toLowerCase();

		// Handle document object-specific logic
		if (elementName.equals("dokumentobjekt")) {

			LOGGER.debug("Extracting file information for {} ...", getItem().getValues().get("referansedokumentfil"));

			try {
				String filename = FilenameUtils
						.separatorsToSystem(getItem().getValues().get("referansedokumentfil").toString());
				File document = new File(getXMLFile().getParentFile(), filename);

				String checksum = ChecksumCalculator.getFileSha256Checksum(document);
				boolean isValidPdfA = PDFAValidator.isValidPdfaFile(document);

				String contentType = isValidPdfA ? PDFAValidator.VALID_FILE_TYPE : PDFAValidator.getFileType(document);

				getItem().add(Field.DETECTED_FILE_TYPE, contentType);
				getItem().add(Field.DETECTED_CHECKSUM, checksum);
				getItem().add(Field.IS_VALID_FILE_TYPE, isValidPdfA);

			} catch (IOException ex) {

				throw new SAXException("Could not extract file information: "
						+ getItem().getValues().get("referansedokumentfil"), ex);
			}
		}

		if (elementName.equals(getItem().getItemDef().getName()) && !isInLeafElementWithTheSameName) {
			Storage.get().write(getItem());
			releaseContentHandler();
		} else {
			isInLeafElementWithTheSameName = false;
			getItem().add(elementName, characters.toString());
		}
	}

	/**
	 * Sets the XML context to the specified {@link BaseHandler} class.
	 */
	@Override
	void setContentHandler(BaseHandler handler, String type) throws SAXException {

		Item childItem = handler.getItem();
		ItemDef childItemDef = childItem.getItemDef();

		ItemDef itemDef = getItem() != null ? getItem().getItemDef() : null;

		if (itemDef != null) {

			childItem.add(itemDef.getReferenceName(), getItem().getValues().get("systemid"));
			childItem.setParentId(getItem().getId());

			for (ItemDef relatedItemDefinition : getItemDefs().values()) {

				String itemDefRef = relatedItemDefinition.getReferenceName();

				if (childItemDef.hasFieldWithName(itemDefRef) && !itemDefRef.equals(itemDef.getReferenceName())) {
					childItem.add(itemDefRef, getReferenceFromParent(itemDefRef));
				}
			}
		}

		super.setContentHandler(handler, type);
	}

	private Object getReferenceFromParent(String itemDefRef) {

		BaseHandler handler = this;
		Object referenceValue = null;

		while (handler != null) {
			if (handler.getItem() != null && handler.getItem().getValues().containsKey(itemDefRef)) {
				referenceValue = handler.getItem().getValues().get(itemDefRef);
				break;
			}
			handler = handler.getParentHandler();
		}

		return referenceValue;
	}
}
