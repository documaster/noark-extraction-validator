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
package com.documaster.validator.reporting;

import com.documaster.validator.config.commands.Command;
import com.documaster.validator.config.delegates.ConfigurableReporting;

public abstract class Report<T extends Command<?> & ConfigurableReporting> {

	private T config;

	private String title;

	Report(T config, String title) {

		this.config = config;
		this.title = title;
	}

	protected T getConfig() {

		return config;
	}

	protected String getTitle() {

		return title;
	}

	/**
	 * Generates the report.
	 */
	abstract void generate() throws Exception;
}
