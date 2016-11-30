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
package com.documaster.validator.config.commands;

import java.lang.reflect.Field;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import com.documaster.validator.config.delegates.Delegate;
import com.documaster.validator.config.properties.InternalProperties;

public abstract class Command<T extends InternalProperties> {

	/**
	 * Validates all instances marked with an {@link ParametersDelegate} annotation and of type {@link Delegate}.
	 *
	 * @throws ParameterException
	 * 		if the validation fails
	 */
	public void validate() throws ParameterException {

		for (Field field : getClass().getDeclaredFields()) {

			if (field.getAnnotation(ParametersDelegate.class) != null && field.getType()
					.isAssignableFrom(Delegate.class)) {

				try {
					((Delegate) field.get(this)).validate();
				} catch (IllegalAccessException ex) {
					throw new ParameterException("Could not access delegate", ex);
				}
			}
		}
	}

	/**
	 * Retrieves the command's name.
	 */
	public abstract String getName();

	/**
	 * Retrieves the {@link InternalProperties} associated with the command.
	 */
	public abstract T getProperties() throws Exception;
}
