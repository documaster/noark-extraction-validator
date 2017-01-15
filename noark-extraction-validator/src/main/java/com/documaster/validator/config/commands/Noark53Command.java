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

import java.io.File;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.converters.FileConverter;
import com.documaster.validator.config.delegates.ConfigurableReporting;
import com.documaster.validator.config.delegates.ConfigurableStorage;
import com.documaster.validator.config.delegates.ReportConfiguration;
import com.documaster.validator.config.delegates.StorageConfiguration;
import com.documaster.validator.config.properties.Noark53Properties;
import com.documaster.validator.config.validators.DirectoryValidator;

@Parameters(commandNames = Noark53Command.COMMAND_NAME,
		commandDescription = "Validates a Noark 5.3 extraction package.")
public class Noark53Command extends Command<Noark53Properties> implements ConfigurableReporting, ConfigurableStorage {

	public static final String COMMAND_NAME = "noark53";

	private static final String EXTRACTION_DIRECTORY = "-extraction";
	@Parameter(
			names = EXTRACTION_DIRECTORY, description = "The location of the extraction package",
			required = true, converter = FileConverter.class, validateValueWith = DirectoryValidator.class)
	private File extractionDirectory;

	private static final String IGNORE_NON_COMPLIANT_XML = "-ignore-non-compliant-xml";
	@Parameter(names = IGNORE_NON_COMPLIANT_XML,
			description = "If specified, execution will continue "
					+ "regardless of the compliance of an XML file to its schema")
	private boolean ignoreNonCompliantXML = false;

	@ParametersDelegate
	private ReportConfiguration reportConfiguration = new ReportConfiguration();

	@ParametersDelegate
	private StorageConfiguration storageConfiguration = new StorageConfiguration();

	private Noark53Properties properties;

	public Noark53Command(JCommander argParser) {

		super(argParser);
	}

	public File getExtractionDirectory() {

		return extractionDirectory;
	}

	public boolean getIgnoreNonCompliantXML() {

		return ignoreNonCompliantXML;
	}

	@Override
	public ReportConfiguration getReportConfiguration() {

		return reportConfiguration;
	}

	@Override
	public StorageConfiguration getStorageConfiguration() {

		return storageConfiguration;
	}

	@Override
	public String getName() {

		return COMMAND_NAME;
	}

	@Override
	public Noark53Properties getProperties() throws Exception {

		if (properties == null) {
			properties = new Noark53Properties(COMMAND_NAME, COMMAND_NAME + ".properties");
		}

		return properties;
	}
}
