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

import com.documaster.validator.exceptions.StorageException;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.storage.model.Persistable;
import com.documaster.validator.storage.model.ShutdownSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class StorageWriter extends Thread {

	static final Persistable SHUTDOWN_SIGNAL = new ShutdownSignal() {

	};
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageWriter.class);

	private Exception lastException;

	Exception getLastException() {

		return lastException;
	}

	@Override
	public void run() {

		LOGGER.info("Running Storage writer thread ...");

		lastException = null;
		boolean shutdown = false;

		while (!shutdown) {

			try {

				Persistable persistableObject = Storage.get().nextInWriteQueue();

				if (persistableObject instanceof ShutdownSignal) {
					shutdown = true;
					continue;
				}

				if (ItemDef.class.isAssignableFrom(persistableObject.getClass())) {
					Storage.get().writeItemDef((ItemDef) persistableObject);
				} else if (Item.class.isAssignableFrom(persistableObject.getClass())) {
					Storage.get().writeItem((Item) persistableObject);
				} else {
					throw new StorageException("Unknown storage type: " + persistableObject);
				}
			} catch (Exception ex) {

				Thread.currentThread().interrupt();
				shutdown = true;

				if (ex instanceof InterruptedException) {
					LOGGER.debug("Storage writer was interrupted.");
				} else {
					lastException = ex;
					throw new StorageException("Storage writer encountered an unexpected error", ex);
				}
			}
		}

		LOGGER.info("Storage writer finished successfully.");
	}
}
