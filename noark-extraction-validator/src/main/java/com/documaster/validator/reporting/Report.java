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
package com.documaster.validator.reporting;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.documaster.validator.config.commands.Command;
import com.documaster.validator.config.delegates.ConfigurableReporting;
import com.documaster.validator.validation.collector.ValidationCollector;
import org.apache.commons.lang3.StringUtils;

public abstract class Report<T extends Command<?> & ConfigurableReporting> {

	private T config;

	private String title;

	private ValidationCollector collector;

	Report(T config, ValidationCollector collector, String title) {

		this.config = config;
		this.collector = collector;
		this.title = title;
	}

	protected T getConfig() {

		return config;
	}

	protected ValidationCollector getCollector() {

		return collector;
	}

	protected String getTitle() {

		return title;
	}

	protected String getDefaultReportName() {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
		String now = dateFormat.format(new Date());

		String reportName = !StringUtils.isBlank(getTitle()) ? getTitle() : "Documaster validation report";
		reportName += " " + now;

		return reportName;
	}

	/**
	 * Generates the report.
	 */
	abstract void generate() throws Exception;
}
