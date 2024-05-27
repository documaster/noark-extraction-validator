/**
 * Noark Extraction Validator
 * Copyright (C) 2017, Documaster AS
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
package com.documaster.validator.validation.noark5.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

/**
 * Represents a construct of the {@link Noark5PackageStructure}, i.e. an XML file and its corresponding XSD schemas.
 */
public class Noark5PackageEntity {

	private final Noark5PackageStructure structure;
	private final String xmlFileName;
	private final boolean optional;
	private final Set<String> xsdSchemasNames;

	public Noark5PackageEntity(
			Noark5PackageStructure structure, String xmlFileName, boolean optional, String... schemaNames) {

		Validate.notEmpty(xmlFileName);

		this.structure = structure;
		this.xmlFileName = xmlFileName;
		this.optional = optional;

		Set<String> schemaNamesSet = new HashSet<>();
		Collections.addAll(schemaNamesSet, schemaNames);
		xsdSchemasNames = Collections.unmodifiableSet(schemaNamesSet);
	}

	public String getXmlFileName() {

		return xmlFileName;
	}

	public boolean isOptional() {

		return optional;
	}

	public Set<String> getXsdShemasNames() {

		return xsdSchemasNames;
	}

	public File getXmlFile() {

		return new File(structure.getExtractionDirectory(), xmlFileName);
	}

	/**
	 * Retrieves the related XSD schema files distributed with the extraction package.
	 */
	public List<File> getPackageSchemas() {

		List<File> packageSchemas = new ArrayList<>();

		for (String xsdSchemaName : xsdSchemasNames) {
			packageSchemas.add(new File(structure.getExtractionDirectory(), xsdSchemaName));
		}

		return Collections.unmodifiableList(packageSchemas);
	}

	/**
	 * Retrieves the Noark 5.x XSD schema files.
	 */
	public List<File> getNoarkSchemas() {

		List<File> noarkSchemas = new ArrayList<>();

		for (String xsdSchemaName : xsdSchemasNames) {
			noarkSchemas.add(new File(structure.getNoarkSchemasDirectory(), xsdSchemaName));
		}

		return Collections.unmodifiableList(noarkSchemas);
	}

	/**
	 * Retrieves the custom schema files.
	 * <br/>
	 * The corresponding Noark 5.x XSD schema is returned for each schema not found in custom schemas.
	 */
	public List<File> getCustomSchemas() {

		List<File> customSchemas = new ArrayList<>();

		for (String xsdSchemaName : xsdSchemasNames) {

			// Custom schema
			File schema = new File(structure.getCustomSchemasDirectory(), xsdSchemaName);

			if (!schema.isFile()) {
				// Fallback to Noark schema
				schema = new File(structure.getNoarkSchemasDirectory(), xsdSchemaName);
			}

			customSchemas.add(schema);
		}

		return Collections.unmodifiableList(customSchemas);
	}

	public boolean hasCustomSchemas() {

		return structure.getCustomSchemasDirectory() != null;
	}
}
