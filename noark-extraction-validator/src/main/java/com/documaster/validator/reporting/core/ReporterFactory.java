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
package com.documaster.validator.reporting.core;

import com.documaster.validator.config.delegates.ReporterDelegate;
import com.documaster.validator.exceptions.ReportingException;
import com.documaster.validator.reporting.excel.ExcelReporter;

public class ReporterFactory {

	private ReporterFactory() {
		// Prevent instantiation
	}

	public static Reporter createReporter(ReporterDelegate config, String title) {

		switch (config.getOutputType()) {
			case EXCEL_XLS:
			case EXCEL_XLSX:
				return new ExcelReporter(config.getOutputDir(), config.getOutputType(), title);
			default:
				throw new ReportingException("Unknown report type: " + config.getOutputType());
		}
	}
}
