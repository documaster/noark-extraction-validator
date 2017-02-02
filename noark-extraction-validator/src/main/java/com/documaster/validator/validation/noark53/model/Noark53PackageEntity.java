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
package com.documaster.validator.validation.noark53.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;

/**
 * Represents a construct of the {@link Noark53PackageStructure}, i.e. an XML file and its corresponding XSD schemas.
 */
public class Noark53PackageEntity {

	private final Noark53PackageStructure structure;
	private final String xmlFileName;
	private final boolean optional;
	private final Set<String> xsdSchemasNames;

	Noark53PackageEntity(
			Noark53PackageStructure structure, String xmlFileName, boolean optional, String... schemaNames) {

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
	 * Retrieves the Noark 5.3 XSD schema files.
	 */
	public List<File> getNoarkSchemas() {

		List<File> noarkSchemas = new ArrayList<>();

		for (String xsdSchemaName : xsdSchemasNames) {
			noarkSchemas.add(new File(structure.getNoarkSchemasDirectory(), xsdSchemaName));
		}

		return Collections.unmodifiableList(noarkSchemas);
	}
}
