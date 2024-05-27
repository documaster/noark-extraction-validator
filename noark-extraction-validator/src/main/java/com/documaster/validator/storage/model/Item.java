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
package com.documaster.validator.storage.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * An extension of {@link BaseItem} that adds explicit support for ID, Type, and Parent and that is connected to an
 * {@link ItemDef} definition, thus, making it {@link Persistable}.
 * <p/>
 * In the case of {@link com.documaster.validator.storage.database.DatabaseStorage} {@link Item} corresponds to a
 * table's record.
 */
public class Item extends BaseItem implements Persistable {

	private ItemDef itemDef;

	/**
	 * Creates a new {@link Item} with the specified {@link ItemDef} definition.
	 *
	 * @param itemDef
	 * 		The {@link ItemDef} this {@link Item} belongs to.
	 */
	public Item(ItemDef itemDef) {

		Validate.notNull("The associated item definition cannot be null");

		this.itemDef = itemDef;
		setId();
	}

	public ItemDef getItemDef() {

		return itemDef;
	}

	public Integer getId() {

		return (Integer) getValues().get(Field.INTERNAL_ID);
	}

	private void setId() {

		add(Field.INTERNAL_ID, itemDef.getNextId());
	}

	public void setParentId(Integer parentId) {

		if (parentId != null) {
			add(Field.INTERNAL_PARENT_ID, parentId);
		}
	}

	public void setType(String type) {

		if (!StringUtils.isBlank(type)) {
			add(Field.TYPE, type);
		}
	}

	@Override
	public String toString() {

		return super.toString() + " in " + itemDef.getFullName();
	}
}
