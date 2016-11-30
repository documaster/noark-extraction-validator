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
package com.documaster.validator.reporting.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.documaster.validator.exceptions.ReportingException;
import com.documaster.validator.reporting.core.Reporter;
import com.documaster.validator.reporting.core.ReporterType;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationCollector.ValidationResult;
import com.documaster.validator.validation.collector.ValidationCollector.ValidationStatusCode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelReporter implements Reporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelReporter.class);

	private File outputDir;

	private ReporterType type;

	private String title;

	private Workbook workbook;

	private Sheet summary;

	private int summaryTableRowIndex = 0;

	private Map<StyleName, CellStyle> styles;

	public ExcelReporter(File outputDir, ReporterType type, String title) {

		this.outputDir = outputDir;
		this.type = type;
		this.title = title;
	}

	@Override
	public void createReport() {

		LOGGER.info(MessageFormat.format("Generating {0} report ...", type));

		try {

			boolean isXlsx = type == ReporterType.EXCEL_XLSX;

			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh-mm-ss");
			String now = dateFormat.format(new Date());

			String filename = !StringUtils.isBlank(title) ? title : "Documaster validation report";
			filename += " " + now;
			filename += isXlsx ? ".xlsx" : ".xls";

			try (
					Workbook wb = isXlsx ? new XSSFWorkbook() : new HSSFWorkbook();
					FileOutputStream out = new FileOutputStream(new File(outputDir, filename))) {

				workbook = wb;

				createStyles();
				createSheets();

				wb.write(out);
			}

			LOGGER.info("Report generated.");
		} catch (Exception ex) {
			LOGGER.error("Could not generate Excel report", ex);
		}
	}

	private void createSheets() {

		summary = createSummarySheet();

		for (Map.Entry<String, List<ValidationResult>> entry : ValidationCollector.get().getAllResults().entrySet()) {

			Cell firstGroupCell = updateGroupTable(entry.getKey(), entry.getValue());
			updateSummaryTable(firstGroupCell, entry.getKey());
		}

		placeTotalsInSummaryTable();

		ExcelUtils.autoSizeColumns(summary, 0, 4);
		summary.setColumnWidth(0, 256);

		ExcelUtils.freezePanes(summary, summaryTableRowIndex + 1, 0);
	}

	private Sheet createSummarySheet() {

		Sheet summarySheet = workbook.createSheet("Summary");

		// Place group-by-group summary table headers
		Row summaryHeaderRow = ExcelUtils.createRow(25, summarySheet);
		ExcelUtils.createCell("", 0, styles.get(StyleName.GROUP), summaryHeaderRow);
		ExcelUtils.createCell("Group name", 1, styles.get(StyleName.GROUP), summaryHeaderRow);
		ExcelUtils.createCell("Information", 2, styles.get(StyleName.GROUP), summaryHeaderRow);
		ExcelUtils.createCell("Warnings", 3, styles.get(StyleName.GROUP), summaryHeaderRow);
		ExcelUtils.createCell("Errors", 4, styles.get(StyleName.GROUP), summaryHeaderRow);

		summaryTableRowIndex = summarySheet.getLastRowNum() + 1;

		// Create rows for each group + 1 for total + 1 blank
		for (int i = 0; i < ValidationCollector.get().getAllResults().size() + 2; i++) {
			ExcelUtils.createRow(summarySheet);
		}

		return summarySheet;
	}

	private void updateSummaryTable(Cell groupTitleCell, String groupTitle) {

		ValidationStatusCode groupStatus = ValidationCollector.get().getGroupStatus(groupTitle);

		StyleName styleName = getResultStatusFromValidationStatus(groupStatus);

		// Status column
		ExcelUtils.createCell("", 0, styles.get(styleName), summary.getRow(summaryTableRowIndex));

		// Title column
		ExcelUtils.createCell(
				groupTitle, 1, styles.get(StyleName.RESULT_TITLE), summary.getRow(summaryTableRowIndex),
				groupTitleCell);

		// Information count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getInformationCountIn(groupTitle)), 2,
				summary.getRow(summaryTableRowIndex));

		// Warning count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getWarningCountIn(groupTitle)), 3,
				summary.getRow(summaryTableRowIndex));

		// Error count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getErrorCountIn(groupTitle)), 4,
				summary.getRow(summaryTableRowIndex));

		summaryTableRowIndex++;
	}

	private void placeTotalsInSummaryTable() {

		// Status column
		ExcelUtils.createCell("", 0, styles.get(StyleName.GROUP), summary.getRow(summaryTableRowIndex));

		// Title column
		ExcelUtils.createCell("Total", 1, styles.get(StyleName.RESULT_TITLE), summary.getRow(summaryTableRowIndex));

		// Information count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getTotalInformationCount()), 2,
				summary.getRow(summaryTableRowIndex));

		// Warning count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getTotalWarningCount()), 3,
				summary.getRow(summaryTableRowIndex));

		// Error count column
		ExcelUtils.createCell(
				Integer.toString(ValidationCollector.get().getTotalErrorCount()), 4,
				summary.getRow(summaryTableRowIndex));

		summaryTableRowIndex++;
	}

	private Cell updateGroupTable(String groupTitle, List<ValidationResult> groupResults) {

		Row groupRow = ExcelUtils.createRow(25, summary);

		ExcelUtils.createCell("", 0, styles.get(StyleName.GROUP), groupRow);
		Cell groupTitleCell = ExcelUtils.createCell(groupTitle, 1, styles.get(StyleName.GROUP), groupRow);
		ExcelUtils.createCell("", 2, styles.get(StyleName.GROUP), groupRow);
		ExcelUtils.createCell("", 3, styles.get(StyleName.GROUP), groupRow);
		ExcelUtils.createCell("", 4, styles.get(StyleName.GROUP), groupRow);

		ValidationStatusCode groupStatusCode = ValidationStatusCode.SUCCESS;

		int resultIndex = 0;

		for (ValidationResult result : groupResults) {

			Row resultRow = ExcelUtils.createRow(15, summary);

			// Color-coded status
			StyleName resultStyleName = getResultStatusFromValidationStatus(result.getStatusCode());
			ExcelUtils.createCell("", 0, styles.get(resultStyleName), resultRow);

			// Title
			Cell titleCell = ExcelUtils.createCell(result.getTitle(), 1, styles.get(StyleName.RESULT_TITLE), resultRow);

			Map<String, Cell> cells = createDetailsSheet(result, ++resultIndex, titleCell);

			ExcelUtils.addHyperLink(titleCell, cells.get("firstRow"));

			// Information entries count
			Cell informationCell = ExcelUtils.createCell(
					MessageFormat.format("Information ({0})", result.getInformation().size()), 2,
					styles.get(StyleName.LINK), resultRow);
			ExcelUtils.addHyperLink(informationCell, cells.get("information"));

			// Warning entries count
			Cell warningCell = ExcelUtils.createCell(
					MessageFormat.format("Warnings ({0})", result.getWarnings().size()), 3, styles.get(StyleName.LINK),
					resultRow);
			ExcelUtils.addHyperLink(warningCell, cells.get("warning"));

			// Error entries count
			Cell errorCell = ExcelUtils.createCell(
					MessageFormat.format("Errors ({0})", result.getErrors().size()), 4, styles.get(StyleName.LINK),
					resultRow);
			ExcelUtils.addHyperLink(errorCell, cells.get("error"));

			if (result.getStatusCode().getCode() > groupStatusCode.getCode()) {

				groupStatusCode = result.getStatusCode();
			}
		}

		return groupTitleCell;
	}

	/**
	 * Creates a new sheet from the specified {@link ValidationResult} and returns the row indices
	 * of the "Information", "Warning", and "Error" entries.
	 */
	private Map<String, Cell> createDetailsSheet(ValidationResult result, int resultIndex, Cell referenceCell) {

		Map<String, Cell> headerCells = new HashMap<>();

		String sheetName = result.getIdentifierPrefix() + resultIndex;

		Sheet resultSheet = workbook.createSheet(sheetName);

		// Go back link
		Row goBackToSymmaryRow = ExcelUtils.createRow(25, resultSheet);
		Cell goBackToSummaryCell = ExcelUtils.createCell(
				"<-- Back to Summary", 0, styles.get(StyleName.LINK), goBackToSymmaryRow);
		ExcelUtils.addHyperLink(goBackToSummaryCell, referenceCell);
		headerCells.put("firstRow", goBackToSummaryCell);

		// Title
		Row titleRow = ExcelUtils.createRow(25, resultSheet);
		StyleName resultStyleName = getResultTitleFromValidationStatus(result.getStatusCode());
		ExcelUtils.createCell("Title: " + result.getTitle(), 0, styles.get(resultStyleName), titleRow);

		// Index cells
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

		// Entries
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

		ExcelUtils.autoSizeColumns(resultSheet, 0, 7);
		ExcelUtils.freezePanes(resultSheet, 5, 0);

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
		createSummaryStatusStyle(IndexedColors.ORANGE, StyleName.RESULT_WARNING);
		createSummaryStatusStyle(IndexedColors.RED, StyleName.RESULT_FAILURE);

		// Validation result sheets styles
		createDetailedTitleStyle(IndexedColors.BRIGHT_GREEN, StyleName.RESULT_TITLE_SUCCESS);
		createDetailedTitleStyle(IndexedColors.ORANGE, StyleName.RESULT_TITLE_WARNING);
		createDetailedTitleStyle(IndexedColors.RED, StyleName.RESULT_TITLE_FAILURE);
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

	private static StyleName getResultStatusFromValidationStatus(ValidationStatusCode code) {

		switch (code) {
			case SUCCESS:
				return StyleName.RESULT_SUCCESS;
			case WARNING:
				return StyleName.RESULT_WARNING;
			case ERROR:
				return StyleName.RESULT_FAILURE;
			default:
				throw new ReportingException("Unknown validation status code: " + code);
		}
	}

	private static StyleName getResultTitleFromValidationStatus(ValidationStatusCode code) {

		switch (code) {
			case SUCCESS:
				return StyleName.RESULT_TITLE_SUCCESS;
			case WARNING:
				return StyleName.RESULT_TITLE_WARNING;
			case ERROR:
				return StyleName.RESULT_TITLE_FAILURE;
			default:
				throw new ReportingException("Unknown validation status code: " + code);
		}
	}
}
