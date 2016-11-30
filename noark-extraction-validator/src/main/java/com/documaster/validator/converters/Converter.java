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
package com.documaster.validator.converters;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;

public interface Converter {

	/**
	 * Returns the model that was generated as a result of the conversion.
	 * <p/>
	 *
	 * @return < object name , object >
	 */
	Map<String, ItemDef> getItemDefs() throws Exception;

	/**
	 * Returns the entries that were generated as a result of the conversion.
	 *
	 * @return A list of {@link Item}.
	 */
	List<Item> getItems() throws Exception;

	void convert(List<File> files) throws Exception;
}
