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
package com.documaster.validator.logging;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class StreamRedirection extends OutputStream {

	/**
	 * Silence the specified {@link SystemStream}.
	 */
	public static void silenceSystemStreams(SystemStream... streams) {

		PrintStream silencedStream = new PrintStream(new StreamRedirection());

		for (SystemStream stream : streams) {
			switch (stream) {
				case OUT:
					System.setOut(silencedStream);
					break;
				case ERR:
					System.setErr(silencedStream);
					break;
				default:
					throw new NotImplementedException("Unknown stream: " + stream);
			}
		}
	}

	/**
	 * Redirect the specified {@link SystemStream}.
	 */
	public static void redirectSystemStream(SystemStream stream, Logger logger, LogLevel logLevel) {

		PrintStream targetStream = new PrintStream(new StreamRedirection(logger, logLevel));

		switch (stream) {
			case OUT:
				System.setOut(targetStream);
				break;
			case ERR:
				System.setErr(targetStream);
				break;
			default:
				throw new NotImplementedException("Unknown stream: " + stream);
		}
	}

	/**
	 * Reset the specified {@link SystemStream} to its default value.
	 */
	public static void resetSystemStreams(SystemStream... streams) {

		for (SystemStream stream : streams) {
			switch (stream) {
				case OUT:
					System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
					break;
				case ERR:
					System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
					break;
				default:
					throw new NotImplementedException("Unknown stream: " + stream);
			}
		}
	}

	private final ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
	private final Logger logger;
	private final LogLevel level;

	/**
	 * Creates a new {@link StreamRedirection} that can be used for silencing streams (its logger is null).
	 */
	private StreamRedirection() {

		this(null);
	}

	/**
	 * Creates a new {@link StreamRedirection} that can be used to do a short-hand redirection to the {@link
	 * LogLevel#INFO} level.
	 */
	private StreamRedirection(Logger logger) {

		this(logger, LogLevel.INFO);
	}

	/**
	 * Creates a new {@link StreamRedirection} with the specified {@link Logger} and specified {@link LogLevel}.
	 */
	private StreamRedirection(Logger logger, LogLevel level) {

		Validate.notNull(level, "Logging level cannot be null");

		this.logger = logger;
		this.level = level;
	}

	@Override
	public void write(int b) {

		if (logger == null) {
			out.reset();
			return;
		}

		if (b == '\n') {
			String line = out.toString();
			out.reset();
			switch (level) {
				case TRACE:
					logger.trace(line);
					break;
				case DEBUG:
					logger.debug(line);
					break;
				case ERROR:
					logger.error(line);
					break;
				case INFO:
					logger.info(line);
					break;
				case WARN:
					logger.warn(line);
					break;
			}
		} else {
			out.write(b);
		}
	}

	public enum LogLevel {
		TRACE, DEBUG, INFO, WARN, ERROR
	}

	public enum SystemStream {
		OUT, ERR
	}
}
