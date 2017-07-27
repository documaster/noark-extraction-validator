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
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.Validate;

/**
 * The Noark 5.3 extraction package structure represented as a {@link HashMap} implementation.
 * <p/>
 * {@link Noark53PackageStructure} provides support for optional external schemas apart from the Noark ones and the ones
 * included in the package.
 * <p/>
 * <b>keys: </b> XML file names<br/>
 * <b>values: </b> {@link Noark53PackageEntity}
 */
public class Noark53PackageStructure extends HashMap<String, Noark53PackageEntity> {

	private File extractionDirectory;
	private File noarkSchemasDirectory;
	private File customSchemasDirectory;

	public Noark53PackageStructure(File extractionDirectory, File noarkSchemasDirectory) {

		this(extractionDirectory, noarkSchemasDirectory, null);
	}

	public Noark53PackageStructure(File extractionDirectory, File noarkSchemasDirectory, File customSchemasDirectory) {

		Validate.isTrue(extractionDirectory.isDirectory());
		Validate.isTrue(noarkSchemasDirectory.isDirectory());

		this.extractionDirectory = extractionDirectory;
		this.noarkSchemasDirectory = noarkSchemasDirectory;
		this.customSchemasDirectory = customSchemasDirectory;

		put(
				"arkivstruktur.xml",
				new Noark53PackageEntity(this, "arkivstruktur.xml", false, "arkivstruktur.xsd", "metadatakatalog.xsd"));
		put("arkivuttrekk.xml", new Noark53PackageEntity(this, "arkivuttrekk.xml", false, "addml.xsd"));
		put("endringslogg.xml", new Noark53PackageEntity(this, "endringslogg.xml", false, "endringslogg.xsd"));
		put("loependeJournal.xml", new Noark53PackageEntity(this, "loependeJournal.xml", true, "loependeJournal.xsd"));
		put(
				"offentligJournal.xml",
				new Noark53PackageEntity(this, "offentligJournal.xml", true, "offentligJournal.xsd"));
	}

	public File getExtractionDirectory() {

		return extractionDirectory;
	}

	public File getNoarkSchemasDirectory() {

		return noarkSchemasDirectory;
	}

	public File getCustomSchemasDirectory() {

		return customSchemasDirectory;
	}

	public List<File> getAllNoarkSchemaFiles() {

		List<File> noarkSchemas = new ArrayList<>();

		for (Noark53PackageEntity entity : values()) {
			noarkSchemas.addAll(entity.getNoarkSchemas());
		}

		return Collections.unmodifiableList(noarkSchemas);
	}
}
