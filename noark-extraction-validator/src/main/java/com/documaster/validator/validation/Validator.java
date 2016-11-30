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

/**
 * A base contract for all {@link Validator}s.
 * <p/>
 * Implementations must provide constructor implementations identical to the {@link Validator}'s constructor for use by
 * the {@link ValidationFactory}.
 *
 * @param <T>
 * 		The {@link Command} associated to the {@link Validator}.
 */
public abstract class Validator<T extends Command> {

	private T command;

	public Validator(T command) {

		this.command = command;
	}

	protected T getCommand() {

		return command;
	}

	public abstract void run() throws Exception;
}
