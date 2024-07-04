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
package com.documaster.validator.validation.noark5.parsers;

import java.io.File;
import java.util.Map;

import com.documaster.validator.storage.core.Storage;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.validation.noark5.provider.ValidationGroup;
import org.apache.commons.lang3.Validate;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class BaseHandler extends DefaultHandler {

	private File xmlFile;

	private XMLReader reader;

	private Map<String, ItemDef> itemDefs;

	private Item item;

	private ValidationGroup validationGroup;

	private BaseHandler parentHandler;

	StringBuilder characters = new StringBuilder();

	BaseHandler(File xmlFile, XMLReader reader, Map<String, ItemDef> itemDefs, ValidationGroup validationGroup) {

		Validate.isTrue(xmlFile.isFile());
		Validate.notNull(reader, "The content reader cannot be null");
		Validate.notNull(itemDefs, "The map containing item definitions cannot be null");
		Validate.notNull(validationGroup, "The handler's validation group cannot be null");

		this.xmlFile = xmlFile;
		this.reader = reader;
		this.itemDefs = itemDefs;
		this.validationGroup = validationGroup;
	}

	BaseHandler(BaseHandler parentHandler, ItemDef itemDef, ValidationGroup validationGroup) {

		this(
				parentHandler.getXMLFile(), parentHandler.getReader(), parentHandler.getItemDefs(),
				validationGroup);

		Validate.notNull(parentHandler, "The parent handler cannot be null");
		Validate.notNull(itemDef, "The associated item definition cannot be null");

		this.parentHandler = parentHandler;
		this.item = new Item(itemDef);
	}

	File getXMLFile() {

		return xmlFile;
	}

	XMLReader getReader() {

		return reader;
	}

	Map<String, ItemDef> getItemDefs() {

		return itemDefs;
	}

	Item getItem() {

		return item;
	}

	public ValidationGroup getValidationGroup() {

		return validationGroup;
	}

	BaseHandler getParentHandler() {

		return parentHandler;
	}

	/**
	 * Sets the XML context to the specified {@link BaseHandler} class and type.
	 *
	 * @param handler
	 * 		The {@link BaseHandler} that should be used
	 * @param type
	 * 		The type of the entry
	 */
	void setContentHandler(BaseHandler handler, String type) throws SAXException {

		Item childItem = handler.getItem();
		childItem.setType(type != null ? type : handler.getItem().getItemDef().getName());

		reader.setContentHandler(handler);
	}

	/**
	 * Releases the current XML context by returning control to the parent's XML context.
	 */
	void releaseContentHandler() {

		if (parentHandler != null) {
			reader.setContentHandler(parentHandler);
		}
	}

	@Override
	public void characters(char buf[], int offset, int length) throws SAXException {

		characters.append(new String(buf, offset, length));
	}

	/**
	 * Switches the content handler if an {@link ItemDef} for the specified element name exists.
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		characters.setLength(0);

		String itemDefName = getItemDefNameForElement(qName.toLowerCase());

		if (getItemDefs().containsKey(itemDefName)) {
			setContentHandler(new BaseHandler(this, getItemDefs().get(itemDefName), validationGroup), null);
		}
	}

	/**
	 * Releases the content handler if the element's name is the same as the name of the {@link ItemDef} corresponding
	 * to this handler; updates the corresponding {@link Item} if not.
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		String elementName = qName.toLowerCase();

		// Inner value
		if (!getItemDefs().containsKey(getItemDefNameForElement(elementName))) {
			getItem().add(elementName, characters.toString().trim());
		}

		// Encountered the element closing tag
		if (elementName.toLowerCase().equals(getItem().getItemDef().getName())) {
			Storage.get().write(getItem());
			releaseContentHandler();
		}
	}

	String getItemDefNameForElement(String elementName) {

		return getValidationGroup().getName() + "." + elementName.toLowerCase();
	}
}
