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
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecksumCalculator {

	private static final Logger LOGGER = LoggerFactory.getLogger(PDFAValidator.class);

	public static String getFileSha256Checksum(File file) {

		try (InputStream is = new FileInputStream(file)) {
			return DigestUtils.sha256Hex(is);
		} catch (Exception ex) {
			LOGGER.warn("Could not calculate the checksum of file: " + file.getAbsolutePath(), ex);
			return null;
		}
	}
}
