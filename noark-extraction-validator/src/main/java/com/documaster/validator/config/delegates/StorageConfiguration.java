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
package com.documaster.validator.config.delegates;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.documaster.validator.exceptions.ReportingException;
import com.documaster.validator.storage.core.Storage;

public class StorageConfiguration implements Delegate {

	private static final String STORAGE = "-storage";
	@Parameter(names = STORAGE, description = "The storage type")
	private Storage.StorageType storageType = Storage.StorageType.HSQLDB_IN_MEMORY;

	private static final String DATABASE_NAME = "-db-name";
	@Parameter(names = DATABASE_NAME, description = "The name of the database")
	private String databaseName = "xdb";

	private static final String DATABASE_DIR_LOCATION = "-db-dir-location";
	@Parameter(names = DATABASE_DIR_LOCATION, description = "The location of the dir where the database will be stored")
	private String databaseDirLocation = System.getProperty("java.io.tmpdir") + "/noark-extraction-validator-db";

	private static final String SERVER_LOCATION = "-server-location";
	@Parameter(names = SERVER_LOCATION, description = "The server location")
	private String serverLocation = "localhost";

	public Storage.StorageType getStorageType() {

		return storageType;
	}

	public void setStorageType(Storage.StorageType storageType) {

		this.storageType = storageType;
	}

	public String getDatabaseName() {

		return databaseName;
	}

	public void setDatabaseName(String databaseName) {

		this.databaseName = databaseName;
	}

	public String getDatabaseDirLocation() {

		return databaseDirLocation;
	}

	public void setDatabaseDirLocation(String databaseDirLocation) {

		this.databaseDirLocation = databaseDirLocation;
	}

	public String getServerLocation() {

		return serverLocation;
	}

	public void setServerLocation(String serverLocation) {

		this.serverLocation = serverLocation;
	}

	@Override
	public void validate() {

		if (storageType == null) {

			throw new ParameterException(STORAGE + " must be specified.");
		}

		switch (storageType) {

			case HSQLDB_SERVER:
				if (serverLocation == null) {
					throw new ParameterException(SERVER_LOCATION + " must be specified");
				}
			case HSQLDB_FILE:
				if (databaseDirLocation == null) {
					throw new ParameterException(DATABASE_DIR_LOCATION + " must be specified");
				}
			case HSQLDB_IN_MEMORY:
				if (databaseName == null) {
					throw new ParameterException(DATABASE_NAME + " must be specified");
				}
				break;
			default:
				throw new ReportingException("Unknown storage type: " + storageType);
		}
	}
}
