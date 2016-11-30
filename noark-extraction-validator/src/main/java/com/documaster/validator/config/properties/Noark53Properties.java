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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;

public class Noark53Properties extends InternalProperties {

	private Noark5PackageStructure packageStructure = new Noark5PackageStructure();

	private Map<String, Set<String>> uniqueFieldsMap = new HashMap<>();

	private Map<String, Set<String>> extraFieldsMap = new HashMap<>();

	private Map<String, String> checksums = new HashMap<>();

	public Noark53Properties(String command, String file) throws IOException, ConfigurationException {

		super(command, file);

		setUniqueFieldsPerTable();
		setExtraFieldsPerTable();
		setChecksums();
	}

	/**
	 * Retrieves the Noark 5 package structure.
	 */
	public Noark5PackageStructure getPackageStructure() {

		return packageStructure;
	}

	/**
	 * Retrieves the unique fields in the specified table.
	 *
	 * @param fullTableName
	 * 		The full name of the table for which to list unique fields
	 * @return A set of unique fields for the specified table
	 */
	public Set<String> getUniqueFieldsInTable(String fullTableName) {

		if (uniqueFieldsMap.containsKey(fullTableName)) {
			return Collections.unmodifiableSet(uniqueFieldsMap.get(fullTableName));
		}

		return Collections.emptySet();
	}

	/**
	 * Retrieves the unique fields in all tables.
	 *
	 * @return < full table name, set of fields >
	 */
	public Map<String, Set<String>> getUniqueFieldsMap() {

		return Collections.unmodifiableMap(uniqueFieldsMap);
	}

	private void setUniqueFieldsPerTable() {

		uniqueFieldsMap.putAll(createMapOfSetsForPropertiesWithPrefix("uniqueFields"));
	}

	/**
	 * Retrieves the extra fields in the specified table.
	 *
	 * @param fullTableName
	 * 		The full name of the table for which to list extra fields
	 * @return A set of extra fields for the specified table
	 */
	public Set<String> getExtraFieldsInTable(String fullTableName) {

		if (extraFieldsMap.containsKey(fullTableName)) {
			return Collections.unmodifiableSet(extraFieldsMap.get(fullTableName));
		}

		return Collections.emptySet();
	}

	/**
	 * Retrieves the extra fields in all tables.
	 *
	 * @return < full table name, set of fields >
	 */
	public Map<String, Set<String>> getExtraFieldsMap() {

		return Collections.unmodifiableMap(extraFieldsMap);
	}

	private void setExtraFieldsPerTable() {

		extraFieldsMap.putAll(createMapOfSetsForPropertiesWithPrefix("additionalFields"));
	}

	/**
	 * Retrieves the checksum of the specified file (or returns null if not found).
	 *
	 * @param fileName
	 * 		The name of the file whose checksum should be fetched
	 * @return The SHA256 checksum of the file with that names
	 */
	public String getChecksumFor(String fileName) {

		return checksums.get(fileName);
	}

	private void setChecksums() {

		Iterator<String> keys = getKeys("schemaChecksums");

		while (keys.hasNext()) {

			String key = keys.next();
			String fileName = key.substring(key.indexOf('.') + 1);

			checksums.put(fileName, getString(key));
		}
	}

	private Map<String, Set<String>> createMapOfSetsForPropertiesWithPrefix(String keyPrefix) {

		Map<String, Set<String>> map = new HashMap<>();

		Iterator<String> keys = getKeys(keyPrefix);

		while (keys.hasNext()) {

			String key = keys.next();
			String mapKey = key.substring(key.indexOf('.') + 1);

			if (!map.containsKey(mapKey)) {
				map.put(mapKey, new HashSet<String>());
			}

			for (Object mapValue : getList(key)) {
				map.get(mapKey).add(mapValue.toString());
			}
		}

		return map;
	}

	public static class Noark5PackageStructure {

		private Map<String, Set<String>> structure;

		Noark5PackageStructure() {

			init();
		}

		public Set<String> getXSDSchemasFor(String xmlFileName) {

			return structure.get(xmlFileName);
		}

		public Set<String> getAllXMLFiles() {

			return structure.keySet();
		}

		public Set<String> getAllXSDFiles() {

			Set<String> xsdSchemas = new HashSet<>();

			for (String xmlFile : getAllXMLFiles()) {
				xsdSchemas.addAll(getXSDSchemasFor(xmlFile));
			}

			return xsdSchemas;
		}

		/**
		 * < xml file name , set of associated XSD schemas file names >
		 */
		public Map<String, Set<String>> getPackageStructure() {

			return Collections.unmodifiableMap(structure);
		}

		private void init() {

			structure = new HashMap<>();

			Set<String> archiveStructureXSDs = new HashSet<>();
			archiveStructureXSDs.add("arkivstruktur.xsd");
			archiveStructureXSDs.add("metadatakatalog.xsd");

			structure.put("arkivstruktur.xml", Collections.unmodifiableSet(archiveStructureXSDs));
			structure.put("arkivuttrekk.xml", Collections.singleton("addml.xsd"));
			structure.put("endringslogg.xml", Collections.singleton("endringslogg.xsd"));
			structure.put("loependeJournal.xml", Collections.singleton("loependeJournal.xsd"));
			structure.put("offentligJournal.xml", Collections.singleton("offentligJournal.xsd"));
		}
	}
}
