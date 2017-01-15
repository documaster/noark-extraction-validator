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
package com.documaster.validator.storage.core;

import java.text.MessageFormat;

import com.documaster.validator.config.delegates.StorageConfiguration;
import com.documaster.validator.exceptions.StorageException;
import com.documaster.validator.storage.database.DatabaseStorage;

public final class StorageFactory {

	private StorageFactory() {
		// Prevent instantiation
	}

	public static Storage createPersistence(StorageConfiguration config) {

		switch (config.getStorageType()) {

			case HSQLDB_IN_MEMORY:
				DatabaseStorage persistence = new DatabaseStorage();

				persistence.setConnectionString(MessageFormat.format(
						"jdbc:hsqldb:mem:{0};hsqldb.write_delay=false;shutdown=true;sql.syntax_pgs=true",
						config.getDatabaseName()));
				persistence.setDriver("org.hsqldb.jdbcDriver");
				persistence.setUsername("SA");
				persistence.setPassword("");
				persistence.setRole("dba");

				return persistence;

			case HSQLDB_SERVER:
				DatabaseStorage serverPersistence = new DatabaseStorage();

				serverPersistence.setConnectionString(MessageFormat.format(
						"jdbc:hsqldb:hsql://{0}/{1};hsqldb.write_delay=false;shutdown=true;sql.syntax_pgs=true",
						config.getServerLocation(), config.getDatabaseName()));
				serverPersistence.setDriver("org.hsqldb.jdbcDriver");
				serverPersistence.setUsername("SA");
				serverPersistence.setPassword("");
				serverPersistence.setRole("dba");

				return serverPersistence;

			default:
				throw new StorageException("Concrete storage not implemented: " + config.getStorageType());
		}
	}
}
