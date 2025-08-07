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
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.tika.Tika;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;

public class PDFAValidator {

	public static final String VALID_FILE_TYPE = "application/pdf";

	private static final PDFAFlavour PDFA_FLAVOUR;
	private static final VeraPDFFoundry VERA_PDF_FOUNDRY;

	static {

		VeraGreenfieldFoundryProvider.initialise();
		VERA_PDF_FOUNDRY = Foundries.defaultInstance();
		PDFA_FLAVOUR = PDFAFlavour.fromString("1b");
	}

	public static String getFileType(File file) throws IOException {

		try {
			Tika tika = new Tika();
			return tika.detect(file);
		} catch (Exception ex) {
			return null;
		}
	}

	public static boolean isValidPdfaFile(File file) {

		try (
				InputStream is = Files.newInputStream(file.toPath());
				PDFAParser parser = VERA_PDF_FOUNDRY.createParser(is)) {

			return VERA_PDF_FOUNDRY
					.createValidator(PDFA_FLAVOUR, false)
					.validate(parser)
					.isCompliant();

		} catch (Exception e) {

			return false;
		}
	}
}
