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
package com.documaster.validator.validation.utils;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.preflight.Format;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFAValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(PDFAValidator.class);
	public static final String VALID_FILE_TYPE = "application/pdf";

	public static String getFileType(File file) throws IOException {

		try {
			Tika tika = new Tika();
			return tika.detect(file);
		} catch (Exception ex) {
			return null;
		}
	}

	public static boolean isValidPdfaFile(File file) throws IOException {

		boolean isValidPdfAFile;

		try {

			String contentType = getFileType(file);

			if (contentType.equalsIgnoreCase(VALID_FILE_TYPE)) {

				ValidationResult result;

				PreflightParser parser = new PreflightParser(file);
				parser.parse(Format.PDF_A1B);

				PreflightDocument document = parser.getPreflightDocument();
				document.validate();
				result = document.getResult();

				isValidPdfAFile = result.isValid();

				document.close();

			} else {
				isValidPdfAFile = false;
			}

		} catch (Exception ex) {
			// Silence the exception
			// PDF Box and Preflight are quite verbose and the exceptions can potentially flood the log
			isValidPdfAFile = false;
		}

		return isValidPdfAFile;
	}
}
