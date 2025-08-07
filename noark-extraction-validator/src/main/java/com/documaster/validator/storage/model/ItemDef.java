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

import java.util.HashMap;
import java.util.Map;

import com.documaster.validator.exceptions.ConversionException;
import org.apache.commons.lang3.Validate;

/**
 * An {@link Item} definition describing its fields, name, and type.
 * <p/>
 * In the case of {@link com.documaster.validator.storage.database.DatabaseStorage} {@link ItemDef} corresponds to a
 * table definition.
 */
public class ItemDef implements Persistable {

	private Class<?> baseClass;

	private String groupName;

	private String name;

	private Integer recordCount = 0;

	private Map<String, Field> fields;

	public ItemDef(Class<?> baseClass) {

		Validate.notNull("The item definition's base class cannot be null");

		this.baseClass = baseClass;
		this.groupName = generateGroupName(baseClass);
		this.name = generateName(baseClass);

		createInternalFields();
	}

	public Class<?> getBaseClass() {

		return baseClass;
	}

	public String getGroupName() {

		return groupName;
	}

	public String getName() {

		return name;
	}

	/**
	 * Returns the {@link ItemDef#getGroupName()} + "." + {@link ItemDef#getName()}
	 */
	public String getFullName() {

		return groupName + "." + name;
	}

	public Map<String, Field> getFields() {

		if (fields == null) {

			fields = new HashMap<>();
		}

		return fields;
	}

	public ItemDef addField(String name, Field.FieldType type) {

		name = name.toLowerCase();

		getFields().put(name, new Field(name, type));

		return this;
	}

	public boolean hasFieldWithName(String fieldName) {

		return getFields().keySet().contains(fieldName.toLowerCase());
	}

	/**
	 * Creates a new {@link Field} to refer to the specified {@link ItemDef}.
	 *
	 * @param refItemDef
	 * 		The {@link ItemDef} for which to create a reference {@link Field}.
	 */
	public ItemDef createReferenceTo(ItemDef refItemDef) {

		if (refItemDef != null && !hasFieldWithName(refItemDef.getReferenceName())) {
			addField(refItemDef.getReferenceName(), Field.FieldType.getFromJavaType(String.class));
		}

		return this;
	}

	/**
	 * Returns the name of the reference fields for this item definition.
	 */
	public String getReferenceName() {

		return Field.REFERENCE_PREFIX + name;
	}

	Integer getNextId() {

		return ++recordCount;
	}

	private void createInternalFields() {

		getFields().put(Field.TYPE, new Field(Field.TYPE, Field.FieldType.getFromJavaType(String.class)));
		getFields()
				.put(Field.INTERNAL_ID, new Field(Field.INTERNAL_ID, Field.FieldType.getFromJavaType(Integer.class)));
		getFields().put(
				Field.INTERNAL_PARENT_ID,
				new Field(Field.INTERNAL_PARENT_ID, Field.FieldType.getFromJavaType(Integer.class)));
	}

	private static String generateGroupName(Class<?> cls) {

		Class<?> rootCls = getRootClassOf(cls);

		int lastOccurrenceOfDot = rootCls.getPackage().getName().lastIndexOf(".");

		if (lastOccurrenceOfDot == -1) {
			throw new ConversionException("Cannot build a group name for a class without a package: "
					+ rootCls.getSimpleName());
		}

		return rootCls.getPackage().getName().substring(lastOccurrenceOfDot + 1).toLowerCase().replace(" -.", "_");
	}

	private static String generateName(Class<?> cls) {

		return getRootClassOf(cls).getSimpleName().toLowerCase().replaceAll("[\\s\\-\\.]", "_");
	}

	/**
	 * Get root non-{@link Object}, non-{@link Enum} class of the specified class.
	 *
	 * @return Root class.
	 */
	private static Class<?> getRootClassOf(Class<?> cls) {

		Class<?> rootClass = cls;

		while (rootClass.getSuperclass() != null && !rootClass.getSuperclass().equals(Object.class)
				&& !rootClass.getSuperclass().equals(Enum.class)) {

			rootClass = rootClass.getSuperclass();
		}

		return rootClass;
	}

	@Override
	public boolean equals(Object o) {

		if (o == null) {
			return false;
		}

		if (this == o) {
			return true;
		}

		return o instanceof ItemDef && ((ItemDef) o).getFullName().equals(getFullName());
	}
}
