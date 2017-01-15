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

import java.util.EnumSet;

import com.documaster.validator.config.delegates.ReportConfiguration;
import com.documaster.validator.exceptions.ReportingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportFactory.class);

	private ReportFactory() {
		// Prevent instantiation
	}

	public static void generateReports(ReportConfiguration config, String title) {

		EnumSet<ReportType> reportTypes = EnumSet.noneOf(ReportType.class);
		reportTypes.addAll(config.getOutputTypes());

		for (ReportType outputType : reportTypes) {
			try {

				LOGGER.info("Generating {} report ...", outputType);

				switch (outputType) {
					case EXCEL_XLS:
					case EXCEL_XLSX:
						new ExcelReport(config.getOutputDir(), outputType, title).generate();
						break;
					default:
						throw new ReportingException("Unknown report type: " + outputType);
				}

				LOGGER.info("{} report generated.", outputType);

			} catch (Exception ex) {
				LOGGER.warn("Could not generate report " + outputType, ex);
			}
		}
	}
}
