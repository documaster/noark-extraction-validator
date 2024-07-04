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
package com.documaster.validator.config.commands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import com.documaster.validator.config.delegates.Delegate;
import com.documaster.validator.config.properties.InternalProperties;
import org.apache.commons.lang3.StringUtils;

public abstract class Command<T extends InternalProperties> {

	private JCommander argParser;

	public Command() {

	}

	public Command(JCommander argParser) {

		this.argParser = argParser;
	}

	/**
	 * Validates all {@link ParametersDelegate}s in this {@link Command}.
	 *
	 * @throws ParameterException
	 * 		if the validation fails
	 */
	public void validate() throws ParameterException {

		for (Field field : getClass().getDeclaredFields()) {
			if (field.getAnnotation(ParametersDelegate.class) != null && Delegate.class
					.isAssignableFrom(field.getType())) {
				try {
					field.setAccessible(true);
					((Delegate) field.get(this)).validate();
				} catch (IllegalAccessException ex) {
					throw new ParameterException("Could not access delegate " + field.getName(), ex);
				}
			}
		}
	}

	public ExecutionInfo getExecutionInfo() {

		List<ParameterInfo> parameters = new ArrayList<>();

		if (argParser != null) {
			for (ParameterDescription param : argParser.getCommands().get(getName()).getParameters()) {
				parameters.add(new ParameterInfo(param));
			}
		}

		ExecutionInfo executionInfo = new ExecutionInfo(parameters);
		executionInfo.addGeneralInfo("command", getName());

		return executionInfo;
	}

	/**
	 * Retrieves the command's name.
	 */
	public abstract String getName();

	/**
	 * Retrieves the {@link InternalProperties} associated with the command.
	 */
	public abstract T getProperties() throws Exception;

	/**
	 * Provides execution information.
	 */
	public static class ExecutionInfo {

		private List<ParameterInfo> parameterInfo = new LinkedList<>();

		private Map<String, Object> generalInfo = new HashMap<>();

		private Map<String, String> commandInfo = new HashMap<>();

		ExecutionInfo(List<ParameterInfo> parameterInfo) {

			this.parameterInfo.addAll(parameterInfo);
		}

		/**
		 * Returns an unmodifiable list containing the parameters of this execution.
		 */
		public List<ParameterInfo> getParameterInfo() {

			return Collections.unmodifiableList(parameterInfo);
		}

		/**
		 * Retrieves an unmodifiable map of general (usually static) information about this execution, such as:
		 * <ul>
		 * <li>Build version</li>
		 * <li>Time of execution</li>
		 * </ul>
		 */
		public Map<String, Object> getGeneralInfo() {

			generalInfo.put("version", getClass().getPackage().getImplementationVersion());
			generalInfo.put("generated", new Date());

			return Collections.unmodifiableMap(generalInfo);
		}

		public void addGeneralInfo(String name, String value) {

			generalInfo.put(name, value);
		}

		/**
		 * Retrieves an unmodifiable map of {@link Command}-specific execution information.
		 */
		public Map<String, String> getCommandInfo() {

			return Collections.unmodifiableMap(commandInfo);
		}

		public void addCommandInfo(String title, String value) {

			commandInfo.put(title, value);
		}

	}

	/**
	 * A wrapper around the {@link JCommander} {@link ParameterDescription}.
	 */
	public static class ParameterInfo {

		private String name;
		private Object specifiedValue;
		private Object defaultValue;
		private String description;
		private boolean required;

		ParameterInfo(ParameterDescription param) {

			this.name = !StringUtils.isBlank(param.getNames()) ? param.getNames() : param.getParameterized().getName();
			this.specifiedValue = param.getParameterized().get(param.getObject());
			this.defaultValue = param.getDefault();
			this.description = param.getDescription();
			this.required = param.getParameter().required();
		}

		public String getName() {

			return name;
		}

		public Object getSpecifiedValue() {

			return specifiedValue;
		}

		public Object getDefaultValue() {

			return defaultValue;
		}

		public String getDescription() {

			return description;
		}

		public boolean isRequired() {

			return required;
		}

		public boolean isDefault() {

			return specifiedValue == null || specifiedValue.equals(defaultValue);
		}
	}
}
