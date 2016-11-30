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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

class ExcelUtils {

	private ExcelUtils() {
		// Prevent instantiation
	}

	static Row createRow(Sheet sheet) {

		return createRow(null, sheet);
	}

	static Row createRow(Integer rowHeight, Sheet sheet) {

		int rowIndex = sheet.getLastRowNum() == 0 && sheet.getRow(0) == null ? 0 : sheet.getLastRowNum() + 1;
		return createRow(rowIndex, rowHeight, sheet);
	}

	static Row createRow(int rowIndex, Integer rowHeightInPoints, Sheet sheet) {

		Row row = sheet.createRow(rowIndex);

		if (rowHeightInPoints != null) {
			row.setHeightInPoints(rowHeightInPoints);
		}

		return row;
	}

	static Cell createCell(Object cellValue, int cellIndex, Row row) {

		return createCell(cellValue, cellIndex, null, row);
	}

	static Cell createCell(Object cellValue, int cellIndex, CellStyle style, Row row) {

		return createCell(cellValue, cellIndex, style, row, null);
	}

	static Cell createCell(Object cellValue, int cellIndex, CellStyle style, Row row, Cell linkTo) {

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

	static void addHyperLink(Cell fromCell, Cell toCell) {

		Hyperlink link = createHyperLinkTo(toCell);

		fromCell.setHyperlink(link);
	}

	private static Hyperlink createHyperLinkTo(Cell cell) {

		Hyperlink link = cell.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_DOCUMENT);
		link.setAddress(MessageFormat.format("''{0}''!{1}", cell.getSheet().getSheetName(), cell.getAddress()));

		return link;
	}

	static CellStyle createDefaultCellStyle(Workbook wb) {

		CellStyle style = wb.createCellStyle();

		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

		return style;
	}

	static void addBorderToStyle(EnumSet<BorderPosition> borderPositions, IndexedColors color, CellStyle style) {

		for (BorderPosition position : borderPositions) {

			switch (position) {

				case TOP:
					style.setBorderTop(CellStyle.BORDER_THIN);
					style.setTopBorderColor(color.getIndex());
					break;
				case BOTTOM:
					style.setBorderBottom(CellStyle.BORDER_THIN);
					style.setBottomBorderColor(color.getIndex());
					break;
				case LEFT:
					style.setBorderLeft(CellStyle.BORDER_THIN);
					style.setLeftBorderColor(color.getIndex());
					break;
				case RIGHT:
					style.setBorderRight(CellStyle.BORDER_THIN);
					style.setRightBorderColor(color.getIndex());
					break;
				default:
					throw new ReportingException("Unknown border position: " + position);
			}
		}
	}

	static void addSolidFillToStyle(CellStyle style, IndexedColors color) {

		style.setFillForegroundColor(color.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
	}

	static Font createDefaultFont(Workbook wb, short size, boolean bold) {

		Font font = wb.createFont();

		font.setFontHeightInPoints(size);
		font.setFontName("Calibri");
		font.setBold(bold);

		return font;
	}

	static void autoSizeColumns(Sheet sheet, int startCol, int endCol) {

		for (int i = startCol; i <= endCol; i++) {
			sheet.autoSizeColumn(i);
		}
	}

	static void freezePanes(Sheet sheet, int numberOfRows, int numberOfColumns) {

		sheet.createFreezePane(numberOfColumns, numberOfRows);
	}
}
