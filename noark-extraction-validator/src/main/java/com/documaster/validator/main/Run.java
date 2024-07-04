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
package com.documaster.validator.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.documaster.validator.config.commands.Noark54Command;
import com.documaster.validator.config.commands.Noark55Command;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.documaster.validator.config.commands.Command;
import com.documaster.validator.config.GlobalConfiguration;
import com.documaster.validator.config.commands.Noark53Command;
import com.documaster.validator.validation.ValidationFactory;
import com.documaster.validator.validation.Validator;
import com.documaster.validator.validation.ValidatorType;

public class Run {

	private static final Logger LOGGER = LoggerFactory.getLogger(Run.class);

	public static void main(String... args) throws Exception {

		try {

			Command command = initConfiguration(args);

			Validator validator = ValidationFactory.createValidator(ValidatorType.byName(command.getName()), command);

			validator.run();

		} catch (ParameterException ex) {

			// Do not print the whole stack trace for a parameter exception
			LOGGER.error(ex.getMessage());
			System.exit(1);

		} catch (Exception ex) {

			LOGGER.error("Validation failed with source exception: ", ex);
			System.exit(1);
		}
	}

	private static Command initConfiguration(String... args) throws ConfigurationException, IOException {

		GlobalConfiguration globalConfig = new GlobalConfiguration();
		JCommander argParser = new JCommander(globalConfig);

		Map<String, Command> commands = new HashMap<>();
		commands.put(Noark53Command.COMMAND_NAME, new Noark53Command(argParser));
		commands.put(Noark54Command.COMMAND_NAME, new Noark54Command(argParser));
		commands.put(Noark55Command.COMMAND_NAME, new Noark55Command(argParser));
		for (Command command : commands.values()) {
			argParser.addCommand(command);
		}

		argParser.parse(args);
		if (args.length == 0 || globalConfig.showHelp()) {
			argParser.usage();
			System.exit(0);
		}

		Command command = commands.get(argParser.getParsedCommand());
		command.validate();

		return command;
	}
}
