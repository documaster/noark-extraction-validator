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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

@Parameters(commandNames = Noark54Command.COMMAND_NAME,
		commandDescription = "Validates a Noark 5.4 extraction package.")
public class Noark54Command extends Noark5Command {

	public static final String COMMAND_NAME = "noark54";
	private static final String NOARK_VERSION = "5.4";

	public Noark54Command() {

		super(COMMAND_NAME, NOARK_VERSION);
	}

	public Noark54Command(JCommander argParser) {

		super(argParser, COMMAND_NAME, NOARK_VERSION);
	}
}
