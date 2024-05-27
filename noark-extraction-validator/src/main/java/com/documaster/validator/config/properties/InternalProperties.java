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
package com.documaster.validator.config.properties;

import java.util.List;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * An interface for handling properties files bundled with the application.
 */
public abstract class InternalProperties extends CompositeConfiguration {

	public InternalProperties(List<String> propertyFiles) throws ConfigurationException {

		if (propertyFiles == null || propertyFiles.isEmpty()) {

			throw new IllegalArgumentException("Property files not specified.");
		}

		for (String file : propertyFiles) {

			PropertiesConfiguration configuration =
					new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
							.configure(new Parameters()
									.properties()
									.setURL(this.getClass().getResource(file))
									.setListDelimiterHandler(new DefaultListDelimiterHandler(',')))
							.getConfiguration();

			addConfiguration(configuration);
		}
	}
}
