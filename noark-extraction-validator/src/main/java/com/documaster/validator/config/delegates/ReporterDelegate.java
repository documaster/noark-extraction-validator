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
package com.documaster.validator.config.delegates;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;
import com.documaster.validator.config.validators.DirectoryValidator;
import com.documaster.validator.exceptions.ReportingException;
import com.documaster.validator.reporting.core.ReporterType;

public class ReporterDelegate implements Delegate {

	private static final String OUTPUT_DIR = "-output-dir";
	@Parameter(
			names = OUTPUT_DIR, description = "The validation report output directory",
			converter = FileConverter.class, validateValueWith = DirectoryValidator.class)
	private File outputDir;

	private static final String OUTPUT_TYPE = "-output-type";
	@Parameter(names = OUTPUT_TYPE, description = "The output type of the validation report")
	private ReporterType reportType = ReporterType.EXCEL_XLSX;

	public File getOutputDir() {

		return outputDir;
	}

	public ReporterType getOutputType() {

		return reportType;
	}

	@Override
	public void validate() {

		if (reportType == null) {

			throw new ParameterException(OUTPUT_TYPE + " must be specified.");
		}

		switch (reportType) {
			case EXCEL_XLS:
			case EXCEL_XLSX:
				if (outputDir == null) {
					throw new ParameterException(OUTPUT_DIR + " must be specified");
				}
				break;
			default:
				throw new ReportingException("Unknown report type: " + reportType);
		}

	}
}
