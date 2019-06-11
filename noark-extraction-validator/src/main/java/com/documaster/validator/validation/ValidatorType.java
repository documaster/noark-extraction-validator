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
package com.documaster.validator.validation;

import java.util.HashMap;
import java.util.Map;

import com.documaster.validator.config.commands.Noark53Command;
import com.documaster.validator.config.commands.Noark54Command;

public enum ValidatorType {

	NOARK53(Noark53Command.COMMAND_NAME),
	NOARK54(Noark54Command.COMMAND_NAME);

	private final String name;

	private static final Map<String, ValidatorType> TYPES;

	static {
		TYPES = new HashMap<>();
		for (ValidatorType validatorType : ValidatorType.values()) {
			TYPES.put(validatorType.getName(), validatorType);
		}
	}

	ValidatorType(String name) {

		this.name = name;
	}

	public String getName() {

		return name;
	}

	public static ValidatorType byName(String name) {

		return TYPES.get(name);
	}
}
