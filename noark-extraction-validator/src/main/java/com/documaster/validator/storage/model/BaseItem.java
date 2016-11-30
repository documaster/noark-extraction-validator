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

/**
 * A basic wrapper around a {@link Map}.
 */
public class BaseItem {

	private Map<String, Object> values;

	public Map<String, Object> getValues() {

		if (values == null) {
			values = new HashMap<>();
		}
		return values;
	}

	public BaseItem add(String name, Object value) {

		getValues().put(name.toLowerCase(), value);
		return this;
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder("Item [");

		for (Map.Entry<String, Object> entry : values.entrySet()) {
			builder.append(String.format(" {%s, %s} ", entry.getKey(), entry.getValue()));
		}

		builder.append(" ]");

		return builder.toString();
	}
}
