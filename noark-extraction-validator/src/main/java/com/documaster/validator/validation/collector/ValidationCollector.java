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
package com.documaster.validator.validation.collector;

import java.util.*;

import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.validation.noark53.provider.ValidationGroup;

/**
 * Collects information about the executed validation.
 */
public class ValidationCollector {

	private static ValidationCollector instance;

	private Map<String, List<ValidationResult>> results;

	private int totalInformationCount = 0;

	private int totalWarningCount = 0;

	private int totalErrorCount = 0;

	private Map<String, Integer> informationCount;

	private Map<String, Integer> warningCount;

	private Map<String, Integer> errorCount;

	private Map<String, ValidationStatusCode> statusCodes;

	private ValidationCollector() {

		results = new LinkedHashMap<>();

		informationCount = new HashMap<>();
		warningCount = new HashMap<>();
		errorCount = new HashMap<>();
		statusCodes = new HashMap<>();
	}

	public static ValidationCollector get() {

		if (instance == null) {

			instance = new ValidationCollector();
		}

		return instance;
	}

	public Map<String, List<ValidationResult>> getAllResults() {

		return results;
	}

	public int getTotalInformationCount() {

		return totalInformationCount;
	}

	public int getTotalWarningCount() {

		return totalWarningCount;
	}

	public int getTotalErrorCount() {

		return totalErrorCount;
	}

	public int getInformationCountIn(String group) {

		return informationCount.containsKey(group) ? informationCount.get(group) : 0;
	}

	public int getWarningCountIn(String group) {

		return warningCount.containsKey(group) ? warningCount.get(group) : 0;
	}

	public int getErrorCountIn(String group) {

		return errorCount.containsKey(group) ? errorCount.get(group) : 0;
	}

	public ValidationStatusCode getGroupStatus(String group) {

		return statusCodes.containsKey(group) ? statusCodes.get(group) : null;
	}

	public void collect(ValidationResult result) {

		String group = result.getGroup().value();

		if (results.containsKey(group)) {

			results.get(group).add(result);

		} else {

			results.put(group, new ArrayList<>(Collections.singletonList(result)));
		}

		putOrIncrementMapCounter(informationCount, group, result.getInformation().size());
		putOrIncrementMapCounter(warningCount, group, result.getWarnings().size());
		putOrIncrementMapCounter(errorCount, group, result.getErrors().size());

		totalInformationCount += result.getInformation().size();
		totalWarningCount += result.getWarnings().size();
		totalErrorCount += result.getErrors().size();

		if (!statusCodes.containsKey(group) || result.getStatusCode().getCode() > statusCodes.get(group).getCode()) {

			statusCodes.put(group, result.getStatusCode());
		}
	}

	private static void putOrIncrementMapCounter(Map<String, Integer> map, String key, int size) {

		if (map.containsKey(key)) {

			map.put(key, map.get(key) + size);

		} else {

			map.put(key, size);
		}
	}

	public static class ValidationResult {

		private String identifierPrefix;

		private String title;

		private ValidationGroup group;

		private List<BaseItem> information;

		private List<BaseItem> warnings;

		private List<BaseItem> errors;

		private ValidationStatusCode code;

		public ValidationResult(String title, ValidationGroup group) {

			this.identifierPrefix = ValidationGroup.getIdentifierOf(group);

			this.title = title;

			this.group = group;
		}

		public String getIdentifierPrefix() {

			return identifierPrefix;
		}

		public String getTitle() {

			return title;
		}

		public ValidationGroup getGroup() {

			return group;
		}

		public List<BaseItem> getInformation() {

			if (information == null) {

				information = new ArrayList<>();
			}

			return information;
		}

		public void addInformation(BaseItem informationEntry) {

			getInformation().add(informationEntry);
		}

		public void addInformation(List<BaseItem> informationEntries) {

			getInformation().addAll(informationEntries);
		}

		public List<BaseItem> getWarnings() {

			if (warnings == null) {

				warnings = new ArrayList<>();
			}

			return warnings;
		}

		public void addWarning(BaseItem warningEntry) {

			getWarnings().add(warningEntry);
		}

		public void addWarnings(List<BaseItem> warningEntries) {

			getWarnings().addAll(warningEntries);
		}

		public List<BaseItem> getErrors() {

			if (errors == null) {

				errors = new ArrayList<>();
			}

			return errors;
		}

		public void addError(BaseItem errorEntry) {

			getErrors().add(errorEntry);
		}

		public void addErrors(List<BaseItem> errorEntries) {

			getErrors().addAll(errorEntries);
		}

		public ValidationStatusCode getStatusCode() {

			if (code == null) {

				if (errors != null && !errors.isEmpty()) {

					code = ValidationStatusCode.ERROR;

				} else {

					if (warnings != null && !warnings.isEmpty()) {

						code = ValidationStatusCode.WARNING;

					} else {

						code = ValidationStatusCode.SUCCESS;
					}
				}
			}

			return code;
		}
	}

	/**
	 * Defines status codes to use in the validation. Status codes with higher severity have higher
	 * integer value. This is helpful when evaluating whether one status code is more/less severe
	 * than another.
	 */
	public enum ValidationStatusCode {

		SUCCESS(0), WARNING(1), ERROR(2);

		private int code;

		ValidationStatusCode(int code) {

			this.code = code;
		}

		public int getCode() {

			return code;
		}
	}
}
