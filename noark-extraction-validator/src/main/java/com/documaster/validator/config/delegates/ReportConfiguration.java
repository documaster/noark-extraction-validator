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
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;
import com.documaster.validator.config.validators.DirectoryValidator;
import com.documaster.validator.exceptions.ReportingException;
import com.documaster.validator.reporting.ReportType;

import static com.documaster.validator.reporting.ReportType.EXCEL_XLSX;

public class ReportConfiguration implements Delegate {

	private static List<ReportType> defaultReportTypes;

	private static final String OUTPUT_DIR = "-output-dir";
	@Parameter(
			names = OUTPUT_DIR, description = "The validation report output directory",
			converter = FileConverter.class, validateValueWith = DirectoryValidator.class)
	private File outputDir;

	private static final String OUTPUT_TYPE = "-output-type";
	@Parameter(names = OUTPUT_TYPE,
			description = "The output type of the validation report. Possible values: XML, EXCEL_XLS, EXCEL_XLSX")
	private List<ReportType> outputTypes = defaultReportTypes;

	private static final String IN_MEMORY_XLSX_REPORT = "-in-memory-xlsx-report";
	@Parameter(names = IN_MEMORY_XLSX_REPORT, description = "Generate the XLSX report using the older, more " +
			"memory consuming fashion (can be used for backwards compatibility)", hidden = true)
	private boolean inMemoryXlsxReport;

	static {
		defaultReportTypes = new ArrayList<>();
		defaultReportTypes.add(EXCEL_XLSX);
	}

	public File getOutputDir() {

		return outputDir;
	}

	public void setOutputDir(File outputDir) {

		this.outputDir = outputDir;
	}

	public List<ReportType> getOutputTypes() {

		return outputTypes;
	}

	public void setOutputTypes(List<ReportType> outputTypes) {

		this.outputTypes = outputTypes;
	}

	public boolean isInMemoryXlsxReport() {

		return inMemoryXlsxReport;
	}

	public void setInMemoryXlsxReport(boolean inMemoryXlsxReport) {

		this.inMemoryXlsxReport = inMemoryXlsxReport;
	}

	@Override
	public void validate() {

		for (ReportType reportType : outputTypes) {
			switch (reportType) {
				case EXCEL_XLS:
				case EXCEL_XLSX:
				case XML:
					if (outputDir == null) {
						throw new ParameterException(OUTPUT_DIR + " must be specified for " + reportType);
					}
					break;
				default:
					throw new ReportingException("Unknown report type: " + reportType);
			}
		}
	}
}
