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
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public class ExcelReport<T extends Command<?> & ConfigurableReporting> extends Report<T> {

	private ReportType reportType;

	private Workbook workbook;

	private Map<StyleName, CellStyle> styles;

	ExcelReport(T config, ReportType reportType, ValidationCollector collector, String title) {

		super(config, collector, title);
		this.reportType = reportType;
	}

	@Override
	public void generate() throws IOException {

		boolean isXlsx = reportType == ReportType.EXCEL_XLSX;

		String extension = isXlsx ? ".xlsx" : ".xls";
		File report = new File(getConfig().getReportConfiguration().getOutputDir(), getDefaultReportName() + extension);

		try (
				Workbook wb = createAccordingWorkbook(isXlsx);
				FileOutputStream out = new FileOutputStream(report)) {

			workbook = wb;

			createStyles();
			createSheets();

			wb.write(out);
		}
	}

	private Workbook createAccordingWorkbook(boolean isXlsx) {

		if (isXlsx) {

			if (getConfig().getReportConfiguration().isInMemoryXlsxReport()) {

				return new XSSFWorkbook();
			} else {

				return new SXSSFWorkbook(SXSSFWorkbook.DEFAULT_WINDOW_SIZE);
			}
		} else {

			return new HSSFWorkbook();
		}
	}

	private void createSheets() {

		createExecutionInfoSheet();
		Map<String, TotalsPerResultCells> totalsPerResultCellRefs = createSummarySheet();
		createDetailsSheet(totalsPerResultCellRefs);
	}

	/**
	 * Creates a sheet called "Execution" that contains information about the execution.
	 */
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
		for (Command.ParameterInfo parameterInfo : getConfig().getExecutionInfo().getParameterInfo()) {
			if (parameterInfo.isDefault()) {
				// Skip default parameters
				continue;
			}
			Row row = ExcelUtils.createRow(executionInfoSheet);
			ExcelUtils.createCell(parameterInfo.getName(), 0, row);
			ExcelUtils.createCell(parameterInfo.getSpecifiedValue(), 1, row);
			ExcelUtils.createCell(
					!StringUtils.isBlank(parameterInfo.getDescription()) ? parameterInfo.getDescription() : "-",
					2,
					row);
			ExcelUtils.createCell(parameterInfo.isRequired(), 3, row);
		}

		// Command-specific information
		if (!getConfig().getExecutionInfo().getCommandInfo().isEmpty()) {
			getConfig().getExecutionInfo().getCommandInfo().entrySet().forEach(e -> {
				// Empty row
				ExcelUtils.createRow(executionInfoSheet);

				// Header
				ExcelUtils.createCell(e.getKey(), 0, styles.get(StyleName.GROUP),
						ExcelUtils.createRow(25, executionInfoSheet));

				// Value
				Row valueRow = ExcelUtils.createRow(executionInfoSheet);

				// Automatically determine the height of the row (works in MS Excel, but no in LibreOffice)
				CellStyle style = ExcelUtils.createDefaultCellStyle(workbook);
				style.setFont(ExcelUtils.createDefaultFont(workbook, (short) 10, false));
				style.setWrapText(true);

				ExcelUtils.createCell(e.getValue(), 0, style, valueRow);
			});

		}

		ExcelUtils.autoSizeColumns(executionInfoSheet, 0, 4, 50 * 256);
	}

	/**
	 * Creates a sheet called "Summary" that contains two separate tables (one after the other):
	 * <ul>
	 * <li>one to hold "Totals per group" information</li>
	 * <li>one to hold "Totals per result" information</li>
	 * </ul>
	 *
	 * @return a map that holds references to the cells in the "Totals per result" table < Result ID, {@link
	 * TotalsPerResultCells} >
	 */
	private Map<String, TotalsPerResultCells> createSummarySheet() {

		Sheet summarySheet = workbook.createSheet("Summary");

		// Store the group name cells so that we can later create hyperlinks
		Map<String, Cell> totalsPerGroupRefCells = createTotalsPerGroupTable(summarySheet);

		// Store the result cells so that we can later create hyperlinks
		Map<String, TotalsPerResultCells> totalsPerResultCellRefs = createTotalsPerResultTable(
				summarySheet, totalsPerGroupRefCells);

		// Auto-size the columns in the "Summary" sheet
		ExcelUtils.autoSizeColumns(summarySheet, 0, 5);

		// Reduce the default width of the color-coded status column
		summarySheet.setColumnWidth(0, 256);

		return totalsPerResultCellRefs;
	}

	/**
	 * Creates a "Totals per group" table in the "Summary" sheet.
	 *
	 * The table has the following structure:
	 * <table>
	 * <thead>
	 * <th colspan=2>Group name</th><th>Summary</th><th>Information</th><th>Warnings</th><th>Errors</th>
	 * </thead>
	 * <tbody>
	 * <tr>
	 * <td>
	 * Color-coded group status
	 * </td>
	 * <td>
	 * Group name
	 * </td>
	 * <td>
	 * Summary count for group
	 * </td>
	 * <td>
	 * Info count for group
	 * </td>
	 * <td>
	 * Warnings count for group
	 * </td>
	 * <td>
	 * Errors count for group
	 * </td>
	 * </tr>
	 * <tr><td>...</td><td>...</td><td>...</td><td>...</td><td>...</td><td>...</td></tr>
	 * <tr>
	 * <td>-</td>
	 * <td>
	 * Total
	 * </td>
	 * <td>
	 * Total summary count
	 * </td>
	 * <td>
	 * Total info count
	 * </td>
	 * <td>
	 * Total warnings count
	 * </td>
	 * <td>
	 * Total errors count
	 * </td>
	 * </tr>
	 * </tbody>
	 * </table>
	 *
	 * @return a map holding references to the cells in which the group titles were placed
	 */
	private Map<String, Cell> createTotalsPerGroupTable(Sheet summarySheet) {

		insertGroupTotalsHeaders(summarySheet);

		Map<String, Cell> groupNameCells = insertGroupTotalsContent(summarySheet);

		insertGroupTotalsFooter(summarySheet);

		// Empty row after the "Totals per group" table
		Row emptyRow = ExcelUtils.createRow(summarySheet);

		// Freeze the rows containing the "Totals per group" table
		ExcelUtils.freezePanes(summarySheet, emptyRow.getRowNum(), 0);

		return groupNameCells;
	}

	/**
	 * Inserts the "Totals per group" table headers.
	 */
	private void insertGroupTotalsHeaders(Sheet summarySheet) {

		Row summaryHeaderRow = ExcelUtils.createRow(25, summarySheet);
		String[] headers = new String[] { "", "Group name", "Summary", "Information", "Warnings", "Errors" };
		IntStream.range(0, headers.length)
				.forEach(ix -> ExcelUtils.createCell(headers[ix], ix, styles.get(StyleName.GROUP), summaryHeaderRow));
	}

	/**
	 * Inserts a new row in the "Totals per group" table for each group.
	 *
	 * @return a map that holds references to the group title cells in the table.
	 */
	private Map<String, Cell> insertGroupTotalsContent(Sheet summarySheet) {

		Map<String, Cell> groupNameCells = new HashMap<>();

		for (String groupTitle : getCollector().getAllResults().keySet()) {

			Row groupRow = ExcelUtils.createRow(summarySheet);

			ValidationStatus groupStatus = getCollector().getGroupStatus(groupTitle);
			CellStyle groupStatusStyle = getResultStatusStyleFromValidationStatus(groupStatus);

			ExcelUtils.createCell("", 0, groupStatusStyle, groupRow); // Status
			groupNameCells.put(
					groupTitle,
					ExcelUtils.createCell(groupTitle, 1, styles.get(StyleName.RESULT_TITLE), groupRow)); // Title
			ExcelUtils.createCell(getCollector().getSummaryCountIn(groupTitle), 2, groupRow); // Summary
			ExcelUtils.createCell(getCollector().getInformationCountIn(groupTitle), 3, groupRow); // Info
			ExcelUtils.createCell(getCollector().getWarningCountIn(groupTitle), 4, groupRow); // Warnings
			ExcelUtils.createCell(getCollector().getErrorCountIn(groupTitle), 5, groupRow); // Errors
		}

		return groupNameCells;
	}

	/**
	 * Inserts a footer row in the "Totals per group" table containing an overall total.
	 */
	private void insertGroupTotalsFooter(Sheet summarySheet) {

		Row totalsRow = ExcelUtils.createRow(summarySheet);

		ExcelUtils.createCell("", 0, styles.get(StyleName.GROUP), totalsRow); // Status
		ExcelUtils.createCell("Total", 1, styles.get(StyleName.RESULT_TITLE), totalsRow); // Title
		ExcelUtils.createCell(getCollector().getTotalSummaryCount(), 2, totalsRow); // Summary count
		ExcelUtils.createCell(getCollector().getTotalInformationCount(), 3, totalsRow); // Information count
		ExcelUtils.createCell(getCollector().getTotalWarningCount(), 4, totalsRow); // Warnings count
		ExcelUtils.createCell(getCollector().getTotalErrorCount(), 5, totalsRow); // Errors count
	}

	/**
	 * Creates a "Totals per result" table right below the "Totals per group" table in the "Summary" sheet.
	 * <p/>
	 * The table has the following structure for each group:
	 *
	 * <table>
	 * <thead>
	 * <th colspan=6>Group name</th>
	 * </thead>
	 * <tbody>
	 * <tr>
	 * <td>Color-coded status</td>
	 * <td>Result name</td>
	 * <td>Summary count</td>
	 * <td>Info count</td>
	 * <td>Warnings count</td>
	 * <td>Errors count</td>
	 * </tr>
	 * <tr><td>...</td><td>...</td><td>...</td><td>...</td><td>...</td><td>...</td></tr>
	 * </tbody>
	 * </table>
	 *
	 * @return a map that holds references to the cells in the table < Result ID, {@link TotalsPerResultCells} >
	 */
	private Map<String, TotalsPerResultCells> createTotalsPerResultTable(
			Sheet summarySheet, Map<String, Cell> totalsPerGroupRefCells) {

		Map<String, TotalsPerResultCells> tableCells = new HashMap<>();

		for (Map.Entry<String, List<ValidationResult>> group : getCollector().getAllResults().entrySet()) {

			String groupName = group.getKey();
			List<ValidationResult> results = group.getValue();

			// Group header (name)
			Row groupHeaderRow = ExcelUtils.createRow(25, summarySheet);
			String[] headerContent = new String[] { "", groupName, "", "", "", "" };
			IntStream.range(0, 6).forEach(
					i -> ExcelUtils.createCell(headerContent[i], i, styles.get(StyleName.GROUP), groupHeaderRow));

			// Create a hyperlink from the group name in the "Totals per group" table
			// to the group name in the "Totals per result" table
			ExcelUtils.addHyperLink(totalsPerGroupRefCells.get(groupName), groupHeaderRow.getCell(1));

			// Individual results
			for (ValidationResult result : results) {

				TotalsPerResultCells cells = new TotalsPerResultCells();
				cells.setResultId(result.getId());

				Row resultRow = ExcelUtils.createRow(15, summarySheet);

				// Color-coded status
				CellStyle resultStyle = getResultStatusStyleFromValidationStatus(result.getStatus());
				ExcelUtils.createCell("", 0, resultStyle, resultRow);

				// Title
				cells.setTitleCell(ExcelUtils
						.createCell(result.getTitle(), 1, styles.get(StyleName.RESULT_TITLE), resultRow));

				CellStyle linkStyle = styles.get(StyleName.LINK);

				// Summary entries count
				String summaryText = String.format("Summary (%d)", result.getSummary().size());
				cells.setSummaryCell(ExcelUtils.createCell(summaryText, 2, linkStyle, resultRow));

				// Information entries count
				String infoText = String.format("Information (%d)", result.getInformation().size());
				cells.setInfoCell(ExcelUtils.createCell(infoText, 3, linkStyle, resultRow));

				// Warning entries count
				String warningsText = String.format("Warnings (%d)", result.getWarnings().size());
				cells.setWarningsCell(ExcelUtils.createCell(warningsText, 4, linkStyle, resultRow));

				// Error entries count
				String errorsText = String.format("Errors (%d)", result.getErrors().size());
				cells.setErrorsCell(ExcelUtils.createCell(errorsText, 5, linkStyle, resultRow));

				tableCells.put(cells.getResultId(), cells);
			}
		}

		return tableCells;
	}

	/**
	 * Creates a new sheet with detailed result information for each {@link ValidationResult}.
	 * <p/>
	 * The content of the sheet is as follows:
	 * <table>
	 * <thead>
	 * <tr><th><-- Back to summary (hyperlink to the "Summary" sheet)</th></tr>
	 * <tr><th>Result title</th></tr>
	 * <tr><th>Summary (n)</th><th>hyperlink to the summary entries in this sheet</th></tr>
	 * <tr><th>Information (n)</th><th>hyperlink to the information entries in this sheet</th></tr>
	 * <tr><th>Warnings (n)</th><th>hyperlink to the warnings entries in this sheet</th></tr>
	 * <tr><th>Errors (n)</th><th>hyperlink to the errors entries in this sheet)</th></tr>
	 * <tr><th>Test description</th></tr>
	 * </thead>
	 * </table>
	 * <table>
	 * <thead>
	 * <tr><th>Summary (n)</th></tr>
	 * <tr><th>col1</th><th>col2</th></tr>
	 * </thead>
	 * <tbody>
	 * <tr><td>val1</td><td>val2</td></tr>
	 * <tr><td>...</td><td>...</td></tr>
	 * </tbody>
	 * </table>
	 * <table>
	 * <thead>
	 * <tr><th>Information (n)</th></tr>
	 * <tr><th>col1</th><th>col2</th></tr>
	 * </thead>
	 * <tbody>
	 * <tr><td>val1</td><td>val2</td></tr>
	 * <tr><td>...</td><td>...</td></tr>
	 * </tbody>
	 * </table>
	 * <table>
	 * <thead>
	 * <tr><th>Warnings (n)</th></tr>
	 * <tr><th>col1</th><th>col2</th></tr>
	 * </thead>
	 * <tbody>
	 * <tr><td>val1</td><td>val2</td></tr>
	 * <tr><td>...</td><td>...</td></tr>
	 * </tbody>
	 * </table>
	 * <table>
	 * <thead>
	 * <tr><th>Errors (n)</th></tr>
	 * <tr><th>col1</th><th>col2</th></tr>
	 * </thead>
	 * <tbody>
	 * <tr><td>val1</td><td>val2</td></tr>
	 * <tr><td>...</td><td>...</td></tr>
	 * </tbody>
	 * </table>
	 *
	 * @param totalsPerResultCellRefs
	 * 		A map containing references to cells in the "Totals per result" table so that hyperlinks can be created
	 */
	private void createDetailsSheet(Map<String, TotalsPerResultCells> totalsPerResultCellRefs) {

		for (List<ValidationResult> results : getCollector().getAllResults().values()) {
			for (ValidationResult result : results) {

				Sheet resultSheet = workbook.createSheet(result.getId());

				// Hyperlink from the detailed sheet information to the "Summary" sheet
				ExcelUtils.createCell("<-- Back to Summary", 0, styles.get(StyleName.LINK),
						ExcelUtils.createRow(25, resultSheet),
						totalsPerResultCellRefs.get(result.getId()).getTitleCell());

				// Title and hyperlink from the "Summary" sheet to it
				CellStyle resultStyle = getResultTitleStyleFromValidationStatus(result.getStatus());
				Cell titleCell = ExcelUtils
						.createCell(result.getTitle(), 0, resultStyle, ExcelUtils.createRow(25, resultSheet));
				ExcelUtils.addHyperLink(totalsPerResultCellRefs.get(result.getId()).getTitleCell(), titleCell);

				// Index
				int indexRowNumber = resultSheet.getLastRowNum() + 1;
				CellStyle linkStyle = styles.get(StyleName.LINK);
				int[] indexItemCounts = new int[] {
						result.getSummary().size(),
						result.getInformation().size(),
						result.getWarnings().size(),
						result.getErrors().size()
				};
				String[] index = new String[] {
						String.format("Summary (%d)", indexItemCounts[0]),
						String.format("Information (%d)", indexItemCounts[1]),
						String.format("Warnings (%d)", indexItemCounts[2]),
						String.format("Errors (%d)", indexItemCounts[3])
				};
				IntStream.range(0, index.length).forEach(
						ix -> createCellWithAheadOfTimeHyperlink(index, ix, linkStyle, resultSheet, indexItemCounts));

				// Description
				Row descriptionRow = ExcelUtils.createStyledRow(resultSheet, styles.get(StyleName.RESULT_DESCRIPTION));
				descriptionRow.setHeightInPoints(50);
				ExcelUtils.createCell(result.getDescription(), 0, styles.get(StyleName.RESULT_DESCRIPTION),
						descriptionRow);

				// Entries (Summary, Information, Warnings, Errors etc.)
				TotalsPerResultCells totalsPerResultCells = totalsPerResultCellRefs.get(result.getId());

				insertDetailedEntries( // Summary
						resultSheet, result.getSummary(), index[0], totalsPerResultCells.getSummaryCell());

				insertDetailedEntries( // Information
						resultSheet, result.getInformation(), index[1], totalsPerResultCells.getInfoCell());

				insertDetailedEntries( // Warnings
						resultSheet, result.getWarnings(), index[2], totalsPerResultCells.getWarningsCell());

				insertDetailedEntries( // Errors
						resultSheet, result.getErrors(), index[3], totalsPerResultCells.getErrorsCell());

				// Automatically resize all columns in the sheet. The number is arbitrarily chosen
				// since we have no easy way of retrieving the result with maximum number of columns.
				ExcelUtils.autoSizeColumns(resultSheet, 0, 11, 50 * 256);

				// Freeze the first several rows so that the index is always visible
				ExcelUtils.freezePanes(resultSheet, indexRowNumber + index.length, 0);
			}
		}
	}

	private void createCellWithAheadOfTimeHyperlink(
			String[] index, int ix, CellStyle linkStyle, Sheet resultSheet, int[] indexItemCounts) {

		int rowsToSkip = 0;

		for (int i = 0; i < ix; i++) {

			rowsToSkip += indexItemCounts[i];
		}

		// 'A' is the column (always the first one) and 8 is the starting row for the 'Summary' information
		String linkAddress = "A" + (8 + rowsToSkip + ix * 3);

		Cell cell = ExcelUtils.createCell(index[ix], 0, linkStyle, ExcelUtils.createRow(resultSheet));
		// Sets a link to another cell that is not yet but will be created
		cell.setHyperlink(ExcelUtils.createHyperLinkTo(resultSheet, linkAddress));
	}

	/**
	 * Inserts detailed result entries into a details sheet.
	 * <p/>
	 * The method appends the following table to the sheet:
	 * <table>
	 * <thead>
	 * <tr><th>Header</th></tr>
	 * <tr><th>col1</th><th>col2</th></tr>
	 * </thead>
	 * <tbody>
	 * <tr><td>val1</td><td>val2</td></tr>
	 * <tr><td>...</td><td>...</td></tr>
	 * </tbody>
	 * </table>
	 *
	 * @param sheet
	 * 		The sheet in which to insert the entries
	 * @param entries
	 * 		The list of entries to insert
	 * @param header
	 * 		The header to use for the entries
	 * @param summaryCellRef
	 * 		The corresponding Info, Warnings, Errors, etc. {@link Cell} in the "Summary" sheet from which to create a
	 * 		hyperlink to the entries.
	 */
	private void insertDetailedEntries(
			Sheet sheet, List<BaseItem> entries, String header, Cell summaryCellRef) {

		// Create a header cell
		Cell headerCell = ExcelUtils
				.createCell(header, 0, styles.get(StyleName.RESULT_TYPE), ExcelUtils.createRow(25, sheet));

		// Create a hyperlink from the "Summary" sheet to these entries
		ExcelUtils.addHyperLink(summaryCellRef, headerCell);

		// Entries
		if (entries.isEmpty()) {
			ExcelUtils.createCell("No information found.", 0, ExcelUtils.createRow(sheet));
		} else {
			// Get the column names from the first row
			Row headerRow = ExcelUtils.createRow(sheet);
			int headerCellIx = 0;

			for (String fieldName : entries.get(0).getValues().keySet()) {
				ExcelUtils.createCell(fieldName, headerCellIx++, styles.get(StyleName.RESULT_HEADER_ROW), headerRow);
			}

			// Write the entries
			for (BaseItem entry : entries) {
				Row row = ExcelUtils.createRow(15, sheet);

				int cellIx = 0;
				for (Object value : entry.getValues().values()) {
					String cellValue = value == null ? "-" : value.toString();
					ExcelUtils.createCell(cellValue, cellIx++, styles.get(StyleName.RESULT_ROW), row);
				}
			}
		}

		// Create an empty row at the end
		ExcelUtils.createRow(sheet);
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

	/**
	 * Holds a reference to all the {@link Cell}s in a "Totals per result" table row.
	 */
	private static class TotalsPerResultCells {

		String resultId;
		Cell titleCell;
		Cell summaryCell;
		Cell infoCell;
		Cell warningsCell;
		Cell errorsCell;

		String getResultId() {

			return resultId;
		}

		void setResultId(String resultId) {

			this.resultId = resultId;
		}

		Cell getTitleCell() {

			return titleCell;
		}

		void setTitleCell(Cell titleCell) {

			this.titleCell = titleCell;
		}

		Cell getSummaryCell() {

			return summaryCell;
		}

		void setSummaryCell(Cell summaryCell) {

			this.summaryCell = summaryCell;
		}

		Cell getInfoCell() {

			return infoCell;
		}

		void setInfoCell(Cell infoCell) {

			this.infoCell = infoCell;
		}

		Cell getWarningsCell() {

			return warningsCell;
		}

		void setWarningsCell(Cell warningsCell) {

			this.warningsCell = warningsCell;
		}

		Cell getErrorsCell() {

			return errorsCell;
		}

		void setErrorsCell(Cell errorsCell) {

			this.errorsCell = errorsCell;
		}
	}
}
