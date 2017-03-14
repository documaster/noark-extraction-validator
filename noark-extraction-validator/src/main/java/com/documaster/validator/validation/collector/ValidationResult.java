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
package com.documaster.validator.validation.collector;

import java.util.ArrayList;
import java.util.List;

import com.documaster.validator.storage.model.BaseItem;

public class ValidationResult {

	private String id;
	private String title;
	private String description;
	private String groupName;

	private List<BaseItem> summary = new ArrayList<>();
	private List<BaseItem> information = new ArrayList<>();
	private List<BaseItem> warnings = new ArrayList<>();
	private List<BaseItem> errors = new ArrayList<>();

	private ValidationStatus code;

	public ValidationResult(String id, String title, String description, String groupName) {

		this.id = id;
		this.title = title;
		this.description = description;
		this.groupName = groupName;
	}

	public String getId() {

		return id;
	}

	public String getTitle() {

		return title;
	}

	public String getDescription() {

		return description;
	}

	public String getGroupName() {

		return groupName;
	}

	public List<BaseItem> getSummary() {

		return summary;
	}

	public void addSummary(BaseItem summaryEntry) {

		getSummary().add(summaryEntry);
	}

	public void addSummaries(List<BaseItem> summaryEntries) {

		getSummary().addAll(summaryEntries);
	}

	public List<BaseItem> getInformation() {

		return information;
	}

	public void addInformation(BaseItem informationEntry) {

		getInformation().add(informationEntry);
	}

	public void addInformation(List<BaseItem> informationEntries) {

		getInformation().addAll(informationEntries);
	}

	public List<BaseItem> getWarnings() {

		return warnings;
	}

	public void addWarning(BaseItem warningEntry) {

		getWarnings().add(warningEntry);
	}

	public void addWarnings(List<BaseItem> warningEntries) {

		getWarnings().addAll(warningEntries);
	}

	public List<BaseItem> getErrors() {

		return errors;
	}

	public void addError(BaseItem errorEntry) {

		getErrors().add(errorEntry);
	}

	public void addErrors(List<BaseItem> errorEntries) {

		getErrors().addAll(errorEntries);
	}

	public ValidationStatus getStatus() {

		if (code == null) {
			if (errors != null && !errors.isEmpty()) {
				code = ValidationStatus.ERROR;
			} else {
				if (warnings != null && !warnings.isEmpty()) {
					code = ValidationStatus.WARNING;
				} else {
					code = ValidationStatus.SUCCESS;
				}
			}
		}
		return code;
	}
}
