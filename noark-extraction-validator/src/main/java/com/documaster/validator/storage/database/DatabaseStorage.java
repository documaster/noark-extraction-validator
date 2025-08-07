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
package com.documaster.validator.storage.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;

import com.documaster.validator.storage.core.Storage;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.storage.model.Field;
import com.documaster.validator.storage.model.Item;
import com.documaster.validator.storage.model.ItemDef;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseStorage extends Storage {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseStorage.class);

	private static final String[] INCOMING_DATE_PATTERNS = new String[] {
			"yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd" };

	private String driver;

	private String connectionString;

	private String username;

	private String password;

	private String role;

	private Connection conn;

	private List<String> existingSchemas;

	private List<String> existingTables;

	public DatabaseStorage() {

		existingSchemas = new ArrayList<>();
		existingTables = new ArrayList<>();
	}

	public void setDriver(String driver) {

		this.driver = driver;
	}

	public void setConnectionString(String connectionString) {

		this.connectionString = connectionString;
	}

	public void setUsername(String username) {

		this.username = username;
	}

	public void setPassword(String password) {

		this.password = password;
	}

	public void setRole(String role) {

		this.role = role;
	}

	@Override
	public void connect() throws SQLException, ClassNotFoundException {

		LOGGER.debug("Initializing connection to " + connectionString);
		Class.forName(driver);
		conn = DriverManager.getConnection(connectionString, username, password);
		conn.setAutoCommit(false);
	}

	@Override
	public boolean isReadAvailable() {

		return conn != null;
	}

	@Override
	public void writeItemDef(ItemDef itemDef) throws SQLException {

		if (!existingSchemas.contains(itemDef.getGroupName())) {
			createSchema(itemDef.getGroupName());
		}

		if (!existingTables.contains(itemDef.getFullName())) {
			createTable(itemDef);
			createIndices(itemDef);
		}
	}

	@Override
	public void writeItem(Item item) throws SQLException {

		// Get the item's fields
		Set<String> fieldNames = new HashSet<>(item.getValues().keySet());

		// Retain only the ones found in its item definition... ignore the rest
		fieldNames.retainAll(item.getItemDef().getFields().keySet());

		String fields = StringUtils.join(fieldNames, ", ");
		String parameters = StringUtils.join(Collections.nCopies(fieldNames.size(), "?"), ", ");

		String insertStmt = MessageFormat
				.format("INSERT INTO {0} ( {1} ) VALUES ( {2} );", item.getItemDef().getFullName(), fields, parameters);

		try (PreparedStatement statement = conn.prepareStatement(insertStmt)) {

			int i = 0;
			for (String field : fieldNames) {

				Object value = item.getValues().get(field);

				boolean isDate = item.getItemDef().getFields().get(field).getFieldType().isDateType();

				// Make sure all dates are properly formatted as such...
				if (value != null && isDate) {
					try {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date date = DateUtils.parseDate(value.toString(), INCOMING_DATE_PATTERNS);
						value = sdf.format(date);
					} catch (ParseException e) {
						// Ignore unknown patterns
					}
				}
				statement.setObject(++i, value);
			}
			statement.executeUpdate();
			conn.commit();
		}
	}

	@Override
	public void destroy() {

		LOGGER.debug("Closing connection to " + connectionString);
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException ex) {
				LOGGER.warn("Could not close connection to " + connectionString);
			} finally {
				conn = null;
			}
		}
	}

	/**
	 * Fetches the results of the specified query as a list of {@link Item}s.
	 * <p/>
	 * The returned records are non-persistable.
	 *
	 * @param query
	 * 		The query to execute
	 * @return A list of non-persistable {@link Item}s
	 */
	@Override
	public List<BaseItem> fetch(String query) throws Exception {

		List<BaseItem> entries = new ArrayList<>();

		try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(query)) {
			while (rs.next()) {
				BaseItem entry = new BaseItem();
				for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
					entry.add(rs.getMetaData().getColumnLabel(i), rs.getString(i));
				}
				entries.add(entry);
			}
		} catch (Exception ex) {
			LOGGER.error("Could not execute fetch query: " + query);
			throw ex;
		}

		return entries;
	}

	private void createSchema(String schemaName) throws SQLException {

		String createStmt = MessageFormat.format("CREATE SCHEMA {0} AUTHORIZATION {1};", schemaName, role);

		try (PreparedStatement statement = conn.prepareStatement(createStmt)) {

			statement.execute();

			conn.commit();
		}

		existingSchemas.add(schemaName);
	}

	private void createTable(ItemDef object) throws SQLException {

		String createStmt = MessageFormat.format("CREATE TABLE {0} ", object.getFullName());

		List<String> fields = new ArrayList<>();

		for (Field field : object.getFields().values()) {
			fields.add(MessageFormat.format("{0} {1}", field.getName(), field.getFieldType().getSqlType()));
		}

		fields.add(MessageFormat.format("PRIMARY KEY ({0})", Field.INTERNAL_ID));

		createStmt += "(" + StringUtils.join(fields, ", ") + " );";

		try (PreparedStatement statement = conn.prepareStatement(createStmt)) {

			statement.execute();

			conn.commit();
		}

		existingTables.add(object.getFullName());
	}

	private void createIndices(ItemDef itemDef) throws SQLException {

		int indexCount = 0;

		for (Field field : itemDef.getFields().values()) {

			if (field.isReference() || field.isParentId() || (uniqueFields != null && uniqueFields
					.containsKey(itemDef.getFullName()) && uniqueFields.get(itemDef.getFullName())
					.contains(field.getName()))) {

				String createStmt = MessageFormat.format(
						"CREATE INDEX \"{0}{1}\" ON {0} ({2});", itemDef.getFullName(), ++indexCount, field.getName());

				try (Statement statement = conn.createStatement()) {

					statement.execute(createStmt);

					conn.commit();
				}
			}
		}
	}
}
