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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.converters.FileConverter;
import com.documaster.validator.config.delegates.ConfigurableReporting;
import com.documaster.validator.config.delegates.ConfigurableStorage;
import com.documaster.validator.config.delegates.ReportConfiguration;
import com.documaster.validator.config.delegates.StorageConfiguration;
import com.documaster.validator.config.properties.Noark5Properties;
import com.documaster.validator.config.validators.DirectoryValidator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Noark5Command extends Command<Noark5Properties>
		implements ConfigurableReporting, ConfigurableStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(Noark5Command.class);

	private final String commandName;

	private final String noarkVersion;

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

	private static final String CUSTOM_SCHEMA_DESCRIPTION_FILENAME = "description.txt";
	private static final String CUSTOM_SCHEMA_LOCATION = "-custom-schema-location";
	@Parameter(names = CUSTOM_SCHEMA_LOCATION,
			description = "The location of custom schema files. The XML files in the extraction package will be "
					+ "validated against these schemas. If a required schema is missing, the Noark 5 one will be "
					+ "used as fallback. If a " + CUSTOM_SCHEMA_DESCRIPTION_FILENAME
					+ " is found in the directory, its content will be "
					+ "copied to the execution information section in the report.")
	private File customSchemaLocation;

	@ParametersDelegate
	private ReportConfiguration reportConfiguration = new ReportConfiguration();

	@ParametersDelegate
	private StorageConfiguration storageConfiguration = new StorageConfiguration();

	private Noark5Properties properties;

	protected Noark5Command(String commandName, String noarkVersion) {

		super();

		this.commandName = commandName;
		this.noarkVersion = noarkVersion;
	}

	protected Noark5Command(JCommander argParser, String commandName, String noarkVersion) {

		super(argParser);

		this.commandName = commandName;
		this.noarkVersion = noarkVersion;
	}

	public File getExtractionDirectory() {

		return extractionDirectory;
	}

	public void setExtractionDirectory(File extractionDirectory) {

		this.extractionDirectory = extractionDirectory;
	}

	public boolean getIgnoreNonCompliantXML() {

		return ignoreNonCompliantXML;
	}

	public void setIgnoreNonCompliantXML(boolean ignoreNonCompliantXML) {

		this.ignoreNonCompliantXML = ignoreNonCompliantXML;
	}

	public File getCustomSchemaLocation() {

		return customSchemaLocation;
	}

	public void setCustomSchemaLocation(File customSchemaLocation) {

		this.customSchemaLocation = customSchemaLocation;
	}

	@Override
	public ReportConfiguration getReportConfiguration() {

		return reportConfiguration;
	}

	public void setReportConfiguration(ReportConfiguration reportConfiguration) {

		this.reportConfiguration = reportConfiguration;
	}

	@Override
	public StorageConfiguration getStorageConfiguration() {

		return storageConfiguration;
	}

	public void setStorageConfiguration(StorageConfiguration storageConfiguration) {

		this.storageConfiguration = storageConfiguration;
	}

	@Override
	public String getName() {

		return commandName;
	}

	public String getNoarkVersion() {

		return noarkVersion;
	}

	@Override
	public Noark5Properties getProperties() throws Exception {

		if (properties == null) {

			String noark5BaseProperties = "/noark5/noark5.properties";
			String commandSpecificProperties = String.format("/noark5/%s/%s.properties", commandName, commandName);

			properties = new Noark5Properties(Arrays.asList(noark5BaseProperties, commandSpecificProperties));
		}

		return properties;
	}

	@Override
	public ExecutionInfo getExecutionInfo() {

		ExecutionInfo info = super.getExecutionInfo();

		if (customSchemaLocation != null && customSchemaLocation.isDirectory()) {
			File schemaDescriptionFile = new File(customSchemaLocation, CUSTOM_SCHEMA_DESCRIPTION_FILENAME);

			if (schemaDescriptionFile.isFile()) {
				String description;

				try {
					description = FileUtils.readFileToString(schemaDescriptionFile, StandardCharsets.UTF_8);
				} catch (Exception e) {
					description = "Could not extract the custom schema description from " + schemaDescriptionFile;
					LOGGER.warn(description, e);
				}

				info.addCommandInfo("Custom schema description", description);
			}
		}

		return info;
	}

	public String getDefaultSchemaLocation() {

		return "noark5/" + commandName;
	}
}
