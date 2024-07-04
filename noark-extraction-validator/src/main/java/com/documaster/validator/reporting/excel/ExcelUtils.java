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

import java.text.MessageFormat;
import java.util.EnumSet;

import com.documaster.validator.exceptions.ReportingException;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;

public class ExcelUtils {

	private ExcelUtils() {
		// Prevent instantiation
	}

	public static Row createStyledRow(Sheet sheet, CellStyle style) {

		Row row = createRow(sheet);
		row.setRowStyle(style);
		return row;
	}

	public static Row createRow(Sheet sheet) {

		return createRow(null, sheet);
	}

	public static Row createRow(Integer rowHeight, Sheet sheet) {

		int rowIndex = sheet.getLastRowNum() == 0 && sheet.getRow(0) == null ? 0 : sheet.getLastRowNum() + 1;
		return createRow(rowIndex, rowHeight, sheet);
	}

	public static Row createRow(int rowIndex, Integer rowHeightInPoints, Sheet sheet) {

		Row row = sheet.createRow(rowIndex);

		if (rowHeightInPoints != null) {
			row.setHeightInPoints(rowHeightInPoints);
		}

		return row;
	}

	public static Cell createCell(Object cellValue, int cellIndex, Row row) {

		return createCell(cellValue, cellIndex, null, row);
	}

	public static Cell createCell(Object cellValue, int cellIndex, CellStyle style, Row row) {

		return createCell(cellValue, cellIndex, style, row, null);
	}

	public static Cell createCell(Object cellValue, int cellIndex, CellStyle style, Row row, Cell linkTo) {

		Cell cell = row.createCell(cellIndex);
		cell.setCellValue(cellValue.toString());

		if (style != null) {

			cell.setCellStyle(style);
		}

		if (linkTo != null) {

			addHyperLink(cell, linkTo);
		}

		return cell;
	}

	public static void addHyperLink(Cell fromCell, Cell toCell) {

		Hyperlink link = createHyperLinkTo(toCell);

		fromCell.setHyperlink(link);
	}

	private static Hyperlink createHyperLinkTo(Cell cell) {

		return createHyperLinkTo(cell.getSheet(), MessageFormat.format(
				"''{0}''!{1}", cell.getSheet().getSheetName(), cell.getAddress()));
	}

	public static Hyperlink createHyperLinkTo(Sheet sheet, String linkAddress) {

		Hyperlink link = sheet.getWorkbook().getCreationHelper().createHyperlink(HyperlinkType.DOCUMENT);
		link.setAddress(linkAddress);

		return link;
	}

	public static CellStyle createDefaultCellStyle(Workbook wb) {

		CellStyle style = wb.createCellStyle();

		style.setAlignment(HorizontalAlignment.LEFT);
		style.setVerticalAlignment(VerticalAlignment.CENTER);

		return style;
	}

	public static void addBorderToStyle(EnumSet<BorderPosition> borderPositions, IndexedColors color, CellStyle style) {

		for (BorderPosition position : borderPositions) {

			switch (position) {

				case TOP:
					style.setBorderTop(BorderStyle.THIN);
					style.setTopBorderColor(color.getIndex());
					break;
				case BOTTOM:
					style.setBorderBottom(BorderStyle.THIN);
					style.setBottomBorderColor(color.getIndex());
					break;
				case LEFT:
					style.setBorderLeft(BorderStyle.THIN);
					style.setLeftBorderColor(color.getIndex());
					break;
				case RIGHT:
					style.setBorderRight(BorderStyle.THIN);
					style.setRightBorderColor(color.getIndex());
					break;
				default:
					throw new ReportingException("Unknown border position: " + position);
			}
		}
	}

	public static void addSolidFillToStyle(CellStyle style, IndexedColors color) {

		style.setFillForegroundColor(color.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	}

	public static Font createDefaultFont(Workbook wb, short size, boolean bold) {

		Font font = wb.createFont();

		font.setFontHeightInPoints(size);
		font.setFontName("Calibri");
		font.setBold(bold);

		return font;
	}

	public static void autoSizeColumns(Sheet sheet, int startCol, int endCol) {

		autoSizeColumns(sheet, startCol, endCol, null);
	}

	public static void autoSizeColumns(Sheet sheet, int startCol, int endCol, Integer maxWidth) {

		for (int i = startCol; i <= endCol; i++) {
			if (sheet instanceof SXSSFSheet) {
				// Columns need explicit tracking when using SXSSFSheet.class for 'streaming' report generation
				((SXSSFSheet) sheet).trackColumnForAutoSizing(i);
			}
			sheet.autoSizeColumn(i);
			if (maxWidth != null && maxWidth > 0 && sheet.getColumnWidth(i) > maxWidth) {
				sheet.setColumnWidth(i, maxWidth);
			}
		}
	}

	public static void freezePanes(Sheet sheet, int numberOfRows, int numberOfColumns) {

		sheet.createFreezePane(numberOfColumns, numberOfRows);
	}
}
