/**
 * Noark Extraction Validator
 * Copyright (C) 2017, Documaster AS
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
package com.documaster.validator.validation.collector;

/**
 * Defines validation status codes. Status codes with higher severity have higher integer value. This is helpful when
 * evaluating the severity of one status compared to another's.
 */
public enum ValidationStatus {

	SUCCESS(0), WARNING(1), ERROR(2);

	private int code;

	ValidationStatus(int code) {

		this.code = code;
	}

	public boolean isMoreSevereThan(ValidationStatus otherStatus) {

		return code > otherStatus.code;
	}
}
