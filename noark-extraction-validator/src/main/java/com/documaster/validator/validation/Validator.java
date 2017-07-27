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
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationResult;

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

	private final T command;

	private final ValidationCollector collector;

	public Validator(T command, ValidationCollector collector) {

		this.command = command;
		this.collector = collector;
	}

	protected T getCommand() {

		return command;
	}

	public ValidationCollector getCollector() {

		return collector;
	}

	protected void collect(ValidationResult result) {

		collector.collect(result);
	}

	public abstract ValidationCollector run() throws Exception;
}
