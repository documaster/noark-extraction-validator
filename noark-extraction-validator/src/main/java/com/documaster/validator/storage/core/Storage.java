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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.documaster.validator.config.delegates.StorageConfiguration;
import com.documaster.validator.exceptions.StorageException;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import com.documaster.validator.storage.model.Persistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Storage {

	private static final Logger LOGGER = LoggerFactory.getLogger(Storage.class);

	private static Storage instance;

	private StorageWriter writerThread;

	private LinkedBlockingQueue<Persistable> queue = new LinkedBlockingQueue<>();

	/**
	 * < item definition full name , set of unique field names >
	 */
	protected Map<String, Set<String>> uniqueFields;

	protected Storage() {
		// Prevent instantiation
	}

	/**
	 * Creates a new concrete {@link Storage} instance via the {@link StorageFactory}, establishes a connection
	 * to the storage provider, and runs a separate thread that listens for actions.
	 *
	 * @param config
	 * 		The {@link StorageConfiguration} with which to instantiate the {@link Storage}.
	 * @param uniqueFields
	 * 		The name of the unique field in each table
	 */
	public static void init(StorageConfiguration config, Map<String, Set<String>> uniqueFields) throws Exception {

		LOGGER.info("Initializing storage ...");

		instance = StorageFactory.createPersistence(config);
		instance.uniqueFields = uniqueFields;
	}

	public static Storage get() {

		return instance;
	}

	/**
	 * Starts the {@link Storage}'s writer.
	 */
	public void startWriter() {

		writerThread = new StorageWriter();
		writerThread.start();
	}

	private boolean isWriteAvailable() {

		return writerThread != null && writerThread.isAlive();
	}

	/**
	 * Queues the specified {@link Persistable} object for writing.
	 * <p/>
	 * Writing a {@link StorageWriter#SHUTDOWN_SIGNAL} will send a shutdown signal to the writer.
	 */
	public void write(Persistable obj) {

		if (!isWriteAvailable()) {
			throw new StorageException(
					"The Storage writer is not listening. Most probably the writer "
							+ "thread encountered an unexpected error. Please verify the logs for more information.");
		}

		queue.offer(obj);
	}

	Persistable nextInWriteQueue() throws InterruptedException {

		return queue.take();
	}

	/**
	 * Sends a shut down signal to the {@link Storage}'s writer, waits for it to exit, and returns its exit status.
	 *
	 * @return <b>true</b> if the writer exists and no exceptions occurred during execution; <b>false</b> otherwise.
	 */
	public boolean stopWriter() {

		if (writerThread != null && isWriteAvailable()) {
			// Send shut down signal to writer
			write(StorageWriter.SHUTDOWN_SIGNAL);

			// Wait for the listener to exit
			try {
				writerThread.join();
			} catch (InterruptedException ex) {
				// Ignore... someone else has interrupted the writer
			}
		}

		return writerThread != null && writerThread.getLastException() == null;
	}

	public Exception getLastWriterException() {

		return writerThread.getLastException();
	}

	/**
	 * Establishes a connection to the implementation's storage provider.
	 */
	public abstract void connect() throws Exception;

	/**
	 * Indicates whether a connection is established and {@link Storage#fetch} can be invoked to retrieve data from the
	 * {@link Storage} implementation.
	 */
	public abstract boolean isReadAvailable();

	/**
	 * Destroys the connection to the implementation's storage provider.
	 */
	public abstract void destroy();

	protected abstract void writeItemDef(ItemDef itemDef) throws Exception;

	protected abstract void writeItem(Item item) throws Exception;

	/**
	 * Fetches the results of the specified query mapped as a list of {@link BaseItem}s.
	 */
	public abstract List<BaseItem> fetch(String query) throws Exception;

	public enum StorageType {

		HSQLDB_IN_MEMORY, HSQLDB_FILE, HSQLDB_SERVER
	}
}
