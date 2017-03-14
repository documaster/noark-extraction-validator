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

/**
 * Collects information about the executed validation.
 */
public class ValidationCollector {

	private static ValidationCollector instance;

	private Map<String, List<ValidationResult>> results;

	private int totalSummaryCount = 0;
	private int totalInformationCount = 0;
	private int totalWarningCount = 0;
	private int totalErrorCount = 0;

	private Map<String, Integer> summaryCount;
	private Map<String, Integer> informationCount;
	private Map<String, Integer> warningCount;
	private Map<String, Integer> errorCount;

	private Map<String, ValidationStatus> statuses;

	private ValidationCollector() {

		results = new LinkedHashMap<>();

		summaryCount = new HashMap<>();
		informationCount = new HashMap<>();
		warningCount = new HashMap<>();
		errorCount = new HashMap<>();
		statuses = new HashMap<>();
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

	public int getTotalSummaryCount() {

		return totalSummaryCount;
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

	public int getSummaryCountIn(String groupName) {

		return summaryCount.containsKey(groupName) ? summaryCount.get(groupName) : 0;
	}

	public int getInformationCountIn(String groupName) {

		return informationCount.containsKey(groupName) ? informationCount.get(groupName) : 0;
	}

	public int getWarningCountIn(String groupName) {

		return warningCount.containsKey(groupName) ? warningCount.get(groupName) : 0;
	}

	public int getErrorCountIn(String groupName) {

		return errorCount.containsKey(groupName) ? errorCount.get(groupName) : 0;
	}

	public int getTotalResultCountIn(String groupName) {

		return results.get(groupName) != null ? results.get(groupName).size() : 0;
	}

	public ValidationStatus getGroupStatus(String groupName) {

		return statuses.get(groupName);
	}

	public void collect(ValidationResult result) {

		String groupName = result.getGroupName();

		if (results.containsKey(groupName)) {

			results.get(groupName).add(result);

		} else {

			results.put(groupName, new ArrayList<>(Collections.singletonList(result)));
		}

		putOrIncrementMapCounter(summaryCount, groupName, result.getSummary().size());
		putOrIncrementMapCounter(informationCount, groupName, result.getInformation().size());
		putOrIncrementMapCounter(warningCount, groupName, result.getWarnings().size());
		putOrIncrementMapCounter(errorCount, groupName, result.getErrors().size());

		totalSummaryCount += result.getSummary().size();
		totalInformationCount += result.getInformation().size();
		totalWarningCount += result.getWarnings().size();
		totalErrorCount += result.getErrors().size();

		if (!statuses.containsKey(groupName) || result.getStatus().isMoreSevereThan(statuses.get(groupName))) {
			statuses.put(groupName, result.getStatus());
		}
	}

	private static void putOrIncrementMapCounter(Map<String, Integer> map, String key, int size) {

		if (map.containsKey(key)) {
			map.put(key, map.get(key) + size);
		} else {
			map.put(key, size);
		}
	}
}
