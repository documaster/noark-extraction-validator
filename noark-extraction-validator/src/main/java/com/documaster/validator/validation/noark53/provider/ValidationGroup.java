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
package com.documaster.validator.validation.noark53.provider;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "group")
@XmlEnum
public enum ValidationGroup {

	@XmlEnumValue("arkivstruktur")
	ARCHIVE_STRUCTURE("arkivstruktur"),

	@XmlEnumValue("loependejournal")
	RUNNING_JOURNAL("loependejournal"),

	@XmlEnumValue("offentligjournal")
	PUBLIC_JOURNAL("offentligjournal"),

	@XmlEnumValue("arkivuttrekk")
	TRANSFER_EXPORTS("addml"),

	@XmlEnumValue("endringslogg")
	CHANGE_LOG("endringslogg"),

	@XmlEnumValue("common")
	COMMON("common");

	private final String value;

	private static Map<String, String> identifierMap;

	static {

		identifierMap = new HashMap<>();

		identifierMap.put(ARCHIVE_STRUCTURE.value(), "AS");
		identifierMap.put(RUNNING_JOURNAL.value(), "LJ");
		identifierMap.put(PUBLIC_JOURNAL.value(), "OJ");
		identifierMap.put(TRANSFER_EXPORTS.value(), "AU");
		identifierMap.put(CHANGE_LOG.value(), "EL");
		identifierMap.put(COMMON.value(), "C");
	}

	ValidationGroup(String v) {

		value = v;
	}

	public String value() {

		return value;
	}

	public static String getIdentifierOf(ValidationGroup group) {

		return identifierMap.get(group.value());
	}
}
