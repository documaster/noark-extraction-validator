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

import com.documaster.validator.exceptions.ConversionException;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.validation.noark5.provider.ValidationGroup;
import org.xml.sax.XMLReader;

public class HandlerFactory {

	private HandlerFactory() {
		// Prevent instantiation
	}

	public static BaseHandler createHandler(File xmlFile, XMLReader reader, Map<String, ItemDef> itemDefs) {

		switch (xmlFile.getName()) {
			case "arkivstruktur.xml":
				return new ArchiveStructureHandler(xmlFile, reader, itemDefs);
			case "arkivuttrekk.xml":
				return new TransferExportsHandler(xmlFile, reader, itemDefs);
			case "endringslogg.xml":
				return new BaseHandler(xmlFile, reader, itemDefs, ValidationGroup.CHANGE_LOG);
			case "loependeJournal.xml":
				return new BaseHandler(xmlFile, reader, itemDefs, ValidationGroup.RUNNING_JOURNAL);
			case "offentligJournal.xml":
				return new BaseHandler(xmlFile, reader, itemDefs, ValidationGroup.PUBLIC_JOURNAL);
			default:
				throw new ConversionException("Cannot create a handler from file: " + xmlFile);
		}
	}
}
