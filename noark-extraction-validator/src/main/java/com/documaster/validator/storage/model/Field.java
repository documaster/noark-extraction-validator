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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.xml.datatype.XMLGregorianCalendar;

public class Field {

	public static final String INTERNAL_ID = "_id";
	public static final String DETECTED_CHECKSUM = "_detected_checksum";
	public static final String DETECTED_FILE_SIZE = "_detected_file_size";
	public static final String DETECTED_FILE_TYPE = "_detected_type";
	public static final String IS_VALID_FILE_TYPE = "_is_valid_type";

	static final String REFERENCE_PREFIX = "_ref_";
	static final String TYPE = "_dtype";
	static final String INTERNAL_PARENT_ID = "_parent_id";

	private String name;

	private FieldType fieldType;

	Field(String name, FieldType fieldType) {

		this.name = name;

		this.fieldType = fieldType;
	}

	public String getName() {

		return name;
	}

	public FieldType getFieldType() {

		return fieldType;
	}

	public boolean isReference() {

		return name.startsWith(REFERENCE_PREFIX);
	}

	public boolean isType() {

		return name.startsWith(TYPE);
	}

	public boolean isParentId() {

		return name.startsWith(INTERNAL_PARENT_ID);
	}

	public enum FieldType {

		BOOLEAN_PRIMITIVE(boolean.class, "SMALLINT"),

		BOOLEAN_OBJECT(Boolean.class, "SMALLINT"),

		BYTE_PRIMITIVE(byte.class, "SMALLINT"),

		BYTE_OBJECT(Byte.class, "SMALLINT"),

		SHORT_PRIMITIVE(short.class, "SMALLINT"),

		SHORT_OBJECT(Short.class, "SMALLINT"),

		INTEGER_PRIMITIVE(int.class, "INTEGER"),

		INTEGER_OBJECT(Integer.class, "INTEGER"),

		LONG_PRIMITIVE(long.class, "BIGINT"),

		LONG_OBJECT(Long.class, "BIGINT"),

		FLOAT_PRIMITIVE(float.class, "REAL"),

		FLOAT_OBJECT(Float.class, "REAL"),

		DOUBLE_PRIMITIVE(double.class, "DOUBLE"),

		DOUBLE_OBJECT(Double.class, "DOUBLE"),

		BIG_INTEGER(BigInteger.class, "BIGINT"),

		BIG_DECIMAL(BigDecimal.class, "DECIMAL"),

		DATE(Date.class, "DATE"), TIME(Time.class, "TIME"),

		TIMESTAMP(Timestamp.class, "TIMESTAMP"),

		XML_GREGORIAN_CALENDAR(XMLGregorianCalendar.class, "TIMESTAMP"),

		CALENDAR(Calendar.class, "TIMESTAMP"),

		BYTE_PRIMITIVE_ARRAY(byte[].class, "VARBINARY"),

		STRING(String.class, "TEXT"),

		OBJECT(Object.class, "TEXT");

		private Class<?> javaType;

		private String sqlType;

		private static final EnumSet<FieldType> dateTypes = EnumSet
				.of(DATE, TIMESTAMP, XML_GREGORIAN_CALENDAR, CALENDAR);

		private static Map<Class<?>, FieldType> classMap;

		static {

			classMap = new HashMap<>();

			classMap.put(boolean.class, BOOLEAN_PRIMITIVE);
			classMap.put(Boolean.class, BOOLEAN_OBJECT);

			classMap.put(byte.class, BYTE_PRIMITIVE);
			classMap.put(Byte.class, BYTE_OBJECT);

			classMap.put(short.class, SHORT_PRIMITIVE);
			classMap.put(Short.class, SHORT_OBJECT);

			classMap.put(int.class, INTEGER_PRIMITIVE);
			classMap.put(Integer.class, INTEGER_OBJECT);

			classMap.put(long.class, LONG_PRIMITIVE);
			classMap.put(Long.class, LONG_OBJECT);

			classMap.put(float.class, FLOAT_PRIMITIVE);
			classMap.put(Float.class, FLOAT_OBJECT);

			classMap.put(double.class, DOUBLE_PRIMITIVE);
			classMap.put(Double.class, DOUBLE_OBJECT);

			classMap.put(BigInteger.class, BIG_INTEGER);
			classMap.put(BigDecimal.class, BIG_DECIMAL);

			classMap.put(Date.class, DATE);
			classMap.put(Time.class, TIME);
			classMap.put(Timestamp.class, TIMESTAMP);
			classMap.put(XMLGregorianCalendar.class, XML_GREGORIAN_CALENDAR);
			classMap.put(Calendar.class, CALENDAR);

			classMap.put(byte[].class, BYTE_PRIMITIVE_ARRAY);

			// Text might not be well-optimized on some engines
			classMap.put(String.class, STRING);

			classMap.put(Object.class, OBJECT);
		}

		FieldType(Class<?> javaType, String sqlType) {

			this.javaType = javaType;
			this.sqlType = sqlType;
		}

		public Class<?> getJavaType() {

			return javaType;
		}

		public String getSqlType() {

			return sqlType;
		}

		public boolean isDateType() {

			return dateTypes.contains(this);
		}

		public static FieldType getFromJavaType(Class<?> javaClass) {

			return classMap.get(javaClass);
		}
	}
}
