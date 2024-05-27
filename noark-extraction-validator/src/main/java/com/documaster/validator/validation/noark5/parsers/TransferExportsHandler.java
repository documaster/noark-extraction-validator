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
import java.util.HashMap;
import java.util.Map;

import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.validation.noark5.provider.ValidationGroup;
import org.apache.commons.lang3.NotImplementedException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A handler for Noark 5 arkivuttrekk.xml file.
 */
class TransferExportsHandler extends BaseHandler {

	TransferExportsHandler(File xmlFile, XMLReader reader, Map<String, ItemDef> itemDefs) {

		super(xmlFile, reader, itemDefs, ValidationGroup.TRANSFER_EXPORTS);
	}

	private TransferExportsHandler(TransferExportsHandler parentHandler, ItemDef itemDef) {

		super(parentHandler, itemDef, ValidationGroup.TRANSFER_EXPORTS);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		characters.setLength(0);
		String elementName = qName.toLowerCase();

		if (getItemDefs().containsKey(getValidationGroup().getName() + "." + elementName)) {

			Map<String, String> attributeMap = new HashMap<>();

			for (int i = 0; i < attributes.getLength(); i++) {

				attributeMap.put(attributes.getQName(i).toLowerCase(), attributes.getValue(i));
			}

			setContentHandler(getItemDefs().get(getItemDefNameForElement(elementName)), attributeMap);
		}
	}

	@Override
	void setContentHandler(BaseHandler handler, String type) {

		throw new NotImplementedException(
				"Use setContentHandler(ItemDef itemDef, Map<String, String> attributes) instead");
	}

	private void setContentHandler(ItemDef itemDef, Map<String, String> attributes) {

		TransferExportsHandler childHandler = new TransferExportsHandler(this, itemDef);

		Item childItem = childHandler.getItem();

		for (String key : attributes.keySet()) {
			if (childHandler.getItem().getItemDef().hasFieldWithName(key)) {
				childItem.add(key, attributes.get(key));
			}
		}

		if (getItem() != null) {
			childItem.setParentId(getItem().getId());
		}

		getReader().setContentHandler(childHandler);
	}
}
