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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.documaster.validator.config.commands.Command;
import com.documaster.validator.config.delegates.ConfigurableReporting;
import com.documaster.validator.exceptions.ReportingException;
import com.documaster.validator.reporting.excel.BorderPosition;
import com.documaster.validator.reporting.excel.ExcelUtils;
import com.documaster.validator.reporting.excel.StyleName;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationResult;
import com.documaster.validator.validation.collector.ValidationStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReport<T extends Command<?> & ConfigurableReporting> extends Report<T> {

	private ReportType reportType;

	private Workbook workbook;

	private Sheet summary;

	private int summaryTableRowIndex = 0;

	private Map<StyleName, CellStyle> styles;

	ExcelReport(T config, ReportType reportType, String title) {

		super(config, title);
		this.reportType = reportType;
	}

	@Override
	public void generate() throws IOException {

		boolean isXlsx = reportType == ReportType.EXCEL_XLSX;

		String extension = isXlsx ? ".xlsx" : ".xls";
		File report = new File(getConfig().getReportConfiguration().getOutputDir(), getDefaultReportName() + extension);

		try (
				Workbook wb = isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
				FileOutputStream out = new FileOutputStream(report)) {

			workbook = wb;

			createStyles();
			createSheets();

			wb.write(out);
		}
	}

	private void createSheets() {

		createExecutionInfoSheet();

		summary = createSummarySheet();

		for (Map.Entry<String, List<ValidationResult>> entry : ValidationCollector.get().getAllResults().entrySet()) {

			Cell firstGroupCell = updateGroupTable(entry.getKey(), entry.getValue());
			updateSummaryTable(firstGroupCell, entry.getKey());
		}

		placeTotalsInSummaryTable();

		ExcelUtils.autoSizeColumns(summary, 0, 5);
		summary.setColumnWidth(0, 256);

		ExcelUtils.freezePanes(summary, summaryTableRowIndex + 1, 0);
	}

	private void createExecutionInfoSheet() {

		Sheet executionInfoSheet = workbook.createSheet("Execution");

		// Information section row
		Row infoSectionRow = ExcelUtils.createRow(25, executionInfoSheet);
		ExcelUtils.createCell("Information", 0, styles.get(StyleName.GROUP), infoSectionRow);

		// General Info
		for (Map.Entry<String, Object> generalInfoEntry : getConfig().getExecutionInfo().getGeneralInfo().entrySet()) {
			String key = !StringUtils.isBlank(generalInfoEntry.getKey()) ? generalInfoEntry.getKey() : "-";
			Object value = generalInfoEntry.getValue() != null ? generalInfoEntry.getValue() : "-";

			Row row = ExcelUtils.createRow(executionInfoSheet);
			ExcelUtils.createCell(key, 0, row);
			ExcelUtils.createCell(value, 1, row);
		}

		// Empty row
		ExcelUtils.createRow(executionInfoSheet);

		// Parameters section row
		Row parametersSectionRow = ExcelUtils.createRow(25, executionInfoSheet);
		ExcelUtils.createCell("Parameters", 0, styles.get(StyleName.GROUP), parametersSectionRow);

		// Parameters headers
		Row parametersHeaderRow = ExcelUtils.createRow(executionInfoSheet);
		ExcelUtils.createCell("Name", 0, styles.get(StyleName.RESULT_HEADER_ROW), parametersHeaderRow);
		ExcelUtils.createCell("Value", 1, styles.get(StyleName.RESULT_HEADER_ROW), parametersHeaderRow);
		ExcelUtils.createCell("Description", 2, styles.get(StyleName.RESULT_HEADER_ROW), parametersHeaderRow);
		ExcelUtils.createCell("Required", 3, styles.get(StyleName.RESULT_HEADER_ROW), parametersHeaderRow);

		// Parameters
		for (Command.ParameterInfo parameter : getConfig().getExecutionInfo().getParameterInfo()) {
			if (parameter.isDefault()) {
				// Skip default parameters
				continue;
			}
			Row row = ExcelUtils.createRow(executionInfoSheet);
			ExcelUtils.createCell(parameter.getName(), 0, row);
			ExcelUtils.createCell(parameter.getSpecifiedValue(), 1, row);
			ExcelUtils
					.createCell(!StringUtils.isBlank(parameter.getDescription()) ? parameter.getDescription() : "-", 2,
							row);
			ExcelUtils.createCell(parameter.isRequired(), 3, row);
		}

		ExcelUtils.autoSizeColumns(executionInfoSheet, 0, 4);
	}

	private Sheet createSummarySheet() {

		Sheet summarySheet = workbook.createSheet("Summary");

		// Place group-by-group summary table headers
		Row summaryHeaderRow = ExcelUtils.createRow(25, summarySheet);
		String[] headers = new String[] { "", "Group name", "Summary", "Information", "Warnings", "Errors" };
		IntStream.range(0, headers.length)
				.forEach(ix -> ExcelUtils.createCell(headers[ix], ix, styles.get(StyleName.GROUP), summaryHeaderRow));

		summaryTableRowIndex = summarySheet.getLastRowNum() + 1;

		// Create rows for each group + 1 for total + 1 blank
		for (int i = 0; i < ValidationCollector.get().getAllResults().size() + 2; i++) {
			ExcelUtils.createRow(summarySheet);
		}

		return summarySheet;
	}

	private void updateSummaryTable(Cell groupTitleCell, String groupTitle) {

		ValidationStatus groupStatus = ValidationCollector.get().getGroupStatus(groupTitle);

		CellStyle style = getResultStatusStyleFromValidationStatus(groupStatus);

		// Status column
		ExcelUtils.createCell("", 0, style, summary.getRow(summaryTableRowIndex));

		// Title column
		ExcelUtils.createCell(
				groupTitle, 1, styles.get(StyleName.RESULT_TITLE), summary.getRow(summaryTableRowIndex),
				groupTitleCell);

		// Summary count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getSummaryCountIn(groupTitle)), 2,
				summary.getRow(summaryTableRowIndex));

		// Information count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getInformationCountIn(groupTitle)), 3,
				summary.getRow(summaryTableRowIndex));

		// Warning count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getWarningCountIn(groupTitle)), 4,
				summary.getRow(summaryTableRowIndex));

		// Error count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getErrorCountIn(groupTitle)), 5,
				summary.getRow(summaryTableRowIndex));

		summaryTableRowIndex++;
	}

	private void placeTotalsInSummaryTable() {

		// Status column
		ExcelUtils.createCell("", 0, styles.get(StyleName.GROUP), summary.getRow(summaryTableRowIndex));

		// Title column
		ExcelUtils.createCell("Total", 1, styles.get(StyleName.RESULT_TITLE), summary.getRow(summaryTableRowIndex));

		// Summary count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getTotalSummaryCount()), 2,
				summary.getRow(summaryTableRowIndex));

		// Information count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getTotalInformationCount()), 3,
				summary.getRow(summaryTableRowIndex));

		// Warning count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getTotalWarningCount()), 4,
				summary.getRow(summaryTableRowIndex));

		// Error count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getTotalErrorCount()), 5,
				summary.getRow(summaryTableRowIndex));

		summaryTableRowIndex++;
	}

	private Cell updateGroupTable(String groupTitle, List<ValidationResult> groupResults) {

		Row groupRow = ExcelUtils.createRow(25, summary);

		// Create group header and place the group title in the second cell
		IntStream.range(0, 6).forEach(
				i -> ExcelUtils.createCell(i == 1 ? groupTitle : "", i, styles.get(StyleName.GROUP), groupRow));

		for (ValidationResult result : groupResults) {

			Row resultRow = ExcelUtils.createRow(15, summary);

			// Color-coded status
			CellStyle resultStyle = getResultStatusStyleFromValidationStatus(result.getStatus());
			ExcelUtils.createCell("", 0, resultStyle, resultRow);

			// Title
			Cell titleCell = ExcelUtils.createCell(result.getTitle(), 1, styles.get(StyleName.RESULT_TITLE), resultRow);

			Map<String, Cell> cells = createDetailsSheet(result, titleCell);

			ExcelUtils.addHyperLink(titleCell, cells.get("firstRow"));

			// Summary entries count
			Cell summaryCell = ExcelUtils.createCell(
					MessageFormat.format("Summary ({0})", result.getSummary().size()), 2,
					styles.get(StyleName.LINK), resultRow);
			ExcelUtils.addHyperLink(summaryCell, cells.get("summary"));

			// Information entries count
			Cell informationCell = ExcelUtils.createCell(
					MessageFormat.format("Information ({0})", result.getInformation().size()), 3,
					styles.get(StyleName.LINK), resultRow);
			ExcelUtils.addHyperLink(informationCell, cells.get("information"));

			// Warning entries count
			Cell warningCell = ExcelUtils.createCell(
					MessageFormat.format("Warnings ({0})", result.getWarnings().size()), 4, styles.get(StyleName.LINK),
					resultRow);
			ExcelUtils.addHyperLink(warningCell, cells.get("warning"));

			// Error entries count
			Cell errorCell = ExcelUtils.createCell(
					MessageFormat.format("Errors ({0})", result.getErrors().size()), 5, styles.get(StyleName.LINK),
					resultRow);
			ExcelUtils.addHyperLink(errorCell, cells.get("error"));
		}

		return groupRow.getCell(1); // Title cell
	}

	/**
	 * Creates a new sheet from the specified {@link ValidationResult} and returns the row indices
	 * of the "Summary", "Information", "Warning", and "Error" entries.
	 */
	private Map<String, Cell> createDetailsSheet(ValidationResult result, Cell referenceCell) {

		Map<String, Cell> headerCells = new HashMap<>();

		Sheet resultSheet = workbook.createSheet(result.getId());

		// Go back link
		Row goBackToSymmaryRow = ExcelUtils.createRow(25, resultSheet);
		Cell goBackToSummaryCell = ExcelUtils.createCell(
				"<-- Back to Summary", 0, styles.get(StyleName.LINK), goBackToSymmaryRow);
		ExcelUtils.addHyperLink(goBackToSummaryCell, referenceCell);
		headerCells.put("firstRow", goBackToSummaryCell);

		// Title
		Row titleRow = ExcelUtils.createRow(25, resultSheet);
		CellStyle resultStyle = getResultTitleStyleFromValidationStatus(result.getStatus());
		ExcelUtils.createCell("Title: " + result.getTitle(), 0, resultStyle, titleRow);

		// Index cells
		int summaryEntries = result.getSummary() == null ? 0 : result.getSummary().size();
		String indexSummaryTitle = MessageFormat.format("Summary ({0})", summaryEntries);
		Cell indexSummaryCell = ExcelUtils.createCell(
				indexSummaryTitle, 0, styles.get(StyleName.LINK), ExcelUtils.createRow(resultSheet));

		int informationEntries = result.getInformation() == null ? 0 : result.getInformation().size();
		String indexInformationTitle = MessageFormat.format("Information ({0})", informationEntries);
		Cell indexInformationCell = ExcelUtils.createCell(
				indexInformationTitle, 0, styles.get(StyleName.LINK), ExcelUtils.createRow(resultSheet));

		int warningEntries = result.getWarnings() == null ? 0 : result.getWarnings().size();
		String indexWarningsTitle = MessageFormat.format("Warnings ({0})", warningEntries);
		Cell indexWarningsCell = ExcelUtils.createCell(
				indexWarningsTitle, 0, styles.get(StyleName.LINK), ExcelUtils.createRow(resultSheet));

		int errorEntries = result.getErrors() == null ? 0 : result.getErrors().size();
		String indexErrorsTitle = MessageFormat.format("Errors ({0})", errorEntries);
		Cell indexErrorsCell = ExcelUtils.createCell(
				indexErrorsTitle, 0, styles.get(StyleName.LINK), ExcelUtils.createRow(resultSheet));

		// Description
		Row descriptionRow = ExcelUtils.createStyledRow(resultSheet, styles.get(StyleName.RESULT_DESCRIPTION));
		descriptionRow.setHeightInPoints(50);
		ExcelUtils.createCell(result.getDescription(), 0, styles.get(StyleName.RESULT_DESCRIPTION), descriptionRow);

		// Entries
		Cell summaryTitleCell = writeDetailEntries(resultSheet, result.getSummary(), "Summary");
		headerCells.put("summary", summaryTitleCell);
		ExcelUtils.addHyperLink(indexSummaryCell, summaryTitleCell);
		ExcelUtils.createRow(resultSheet); // empty row

		Cell informationTitleCell = writeDetailEntries(resultSheet, result.getInformation(), "Information");
		headerCells.put("information", informationTitleCell);
		ExcelUtils.addHyperLink(indexInformationCell, informationTitleCell);
		ExcelUtils.createRow(resultSheet); // empty row

		Cell warningsTitleCell = writeDetailEntries(resultSheet, result.getWarnings(), "Warnings");
		headerCells.put("warning", warningsTitleCell);
		ExcelUtils.addHyperLink(indexWarningsCell, warningsTitleCell);
		ExcelUtils.createRow(resultSheet); // empty row

		Cell errorsTitleCell = writeDetailEntries(resultSheet, result.getErrors(), "Errors");
		headerCells.put("error", errorsTitleCell);
		ExcelUtils.addHyperLink(indexErrorsCell, errorsTitleCell);
		ExcelUtils.createRow(resultSheet); // empty row

		resultSheet.setColumnWidth(0, 70 * 256);
		ExcelUtils.autoSizeColumns(resultSheet, 1, 11);
		ExcelUtils.freezePanes(resultSheet, 6, 0);

		return headerCells;
	}

	private Cell writeDetailEntries(Sheet resultSheet, List<BaseItem> entries, String entriesType) {

		// Entries type
		int entriesCount = entries == null ? 0 : entries.size();

		Row typeRow = ExcelUtils.createRow(25, resultSheet);

		String typeContent = MessageFormat.format("{0} ({1})", entriesType, entriesCount);
		Cell typeCell = ExcelUtils.createCell(typeContent, 0, styles.get(StyleName.RESULT_TYPE), typeRow);

		// Entries
		if (entriesCount == 0) {

			Row noResultsRow = ExcelUtils.createRow(resultSheet);
			ExcelUtils.createCell(MessageFormat.format("No {0} found.", entriesType.toLowerCase()), 0, noResultsRow);

		} else {

			Row headerRow = ExcelUtils.createRow(resultSheet);

			int headerCellIndex = 0;

			// Get the table headers from the first row
			for (String fieldName : entries.get(0).getValues().keySet()) {
				ExcelUtils.createCell(fieldName, headerCellIndex++, styles.get(StyleName.RESULT_HEADER_ROW), headerRow);
			}

			// Write values
			for (BaseItem entry : entries) {

				Row row = ExcelUtils.createRow(15, resultSheet);

				int cellIndex = 0;
				for (Object value : entry.getValues().values()) {
					String cellValue = value == null ? "-" : value.toString();
					ExcelUtils.createCell(cellValue, cellIndex++, styles.get(StyleName.RESULT_ROW), row);
				}
			}
		}

		return typeCell;
	}

	private void createStyles() {

		styles = new HashMap<>();

		createLinkStyle();

		// Summary sheet styles
		createSummaryGroupStyle();
		createSummaryTitleStyle();
		createSummaryStatusStyle(IndexedColors.BRIGHT_GREEN, StyleName.RESULT_SUCCESS);
		createSummaryStatusStyle(IndexedColors.LIGHT_ORANGE, StyleName.RESULT_WARNING);
		createSummaryStatusStyle(IndexedColors.RED, StyleName.RESULT_FAILURE);

		// Validation result sheets styles
		createDetailedTitleStyle(IndexedColors.BRIGHT_GREEN, StyleName.RESULT_TITLE_SUCCESS);
		createDetailedTitleStyle(IndexedColors.LIGHT_ORANGE, StyleName.RESULT_TITLE_WARNING);
		createDetailedTitleStyle(IndexedColors.RED, StyleName.RESULT_TITLE_FAILURE);
		createDetailedDescriptionStyle();
		createDetailedTypeStyle();
		createDetailedHeaderRowStyle();
		createDetailedRowStyle();
	}

	private void createLinkStyle() {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		Font font = ExcelUtils.createDefaultFont(workbook, (short) 10, false);
		font.setUnderline(Font.U_SINGLE);
		font.setColor(IndexedColors.BLUE.getIndex());

		style.setFont(font);

		styles.put(StyleName.LINK, style);
	}

	private void createSummaryGroupStyle() {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 11, true));

		ExcelUtils.addSolidFillToStyle(style, IndexedColors.GREY_25_PERCENT);

		ExcelUtils.addBorderToStyle(EnumSet.of(BorderPosition.BOTTOM), IndexedColors.GREY_50_PERCENT, style);

		styles.put(StyleName.GROUP, style);
	}

	private void createSummaryTitleStyle() {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 10, false));
		style.setIndention((short) 1);

		styles.put(StyleName.RESULT_TITLE, style);
	}

	private void createSummaryStatusStyle(IndexedColors color, StyleName styleName) {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		ExcelUtils.addSolidFillToStyle(style, color);

		ExcelUtils.addBorderToStyle(EnumSet.of(BorderPosition.RIGHT, BorderPosition.BOTTOM), color, style);

		styles.put(styleName, style);
	}

	private void createDetailedTitleStyle(IndexedColors color, StyleName styleName) {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 11, true));

		ExcelUtils.addSolidFillToStyle(style, color);

		styles.put(styleName, style);
	}

	private void createDetailedDescriptionStyle() {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 10, false));
		style.setWrapText(true);

		styles.put(StyleName.RESULT_DESCRIPTION, style);
	}

	private void createDetailedTypeStyle() {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 11, true));

		ExcelUtils.addSolidFillToStyle(style, IndexedColors.GREY_25_PERCENT);

		ExcelUtils.addBorderToStyle(EnumSet.of(BorderPosition.BOTTOM), IndexedColors.GREY_50_PERCENT, style);

		styles.put(StyleName.RESULT_TYPE, style);
	}

	private void createDetailedHeaderRowStyle() {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 10, true));
		style.setIndention((short) 1);

		ExcelUtils.addBorderToStyle(EnumSet.of(BorderPosition.BOTTOM), IndexedColors.GREY_50_PERCENT, style);

		styles.put(StyleName.RESULT_HEADER_ROW, style);
	}

	private void createDetailedRowStyle() {

		CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);

		style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 10, false));
		style.setIndention((short) 1);

		styles.put(StyleName.RESULT_ROW, style);
	}

	private CellStyle getResultStatusStyleFromValidationStatus(ValidationStatus status) {

		switch (status) {
			case SUCCESS:
				return styles.get(StyleName.RESULT_SUCCESS);
			case WARNING:
				return styles.get(StyleName.RESULT_WARNING);
			case ERROR:
				return styles.get(StyleName.RESULT_FAILURE);
			default:
				throw new ReportingException("Unknown validation status: " + status);
		}
	}

	private CellStyle getResultTitleStyleFromValidationStatus(ValidationStatus status) {

		switch (status) {
			case SUCCESS:
				return styles.get(StyleName.RESULT_TITLE_SUCCESS);
			case WARNING:
				return styles.get(StyleName.RESULT_TITLE_WARNING);
			case ERROR:
				return styles.get(StyleName.RESULT_TITLE_FAILURE);
			default:
				throw new ReportingException("Unknown validation status: " + status);
		}
	}
}
