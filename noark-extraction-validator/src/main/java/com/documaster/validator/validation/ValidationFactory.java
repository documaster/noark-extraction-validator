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

import com.documaster.validator.config.commands.Command;
import com.documaster.validator.config.commands.Noark53Command;
import com.documaster.validator.exceptions.ValidationException;
import com.documaster.validator.validation.noark53.Noark53Validator;

public class ValidationFactory {

	public static Validator createValidator(ValidatorType validatorType, Command command) {

		switch (validatorType) {
			case NOARK53:
				return new Noark53Validator((Noark53Command) command);
			default:
				throw new ValidationException("Validator not implemented: " + validatorType.getName());
		}
	}
}
