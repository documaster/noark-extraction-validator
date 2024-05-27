/**
 * Noark Extraction Validator
 * Copyright (C) 2017, Documaster AS
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.documaster.validator.config.commands.Command;
import com.documaster.validator.config.delegates.ConfigurableReporting;
import com.documaster.validator.storage.model.BaseItem;
import com.documaster.validator.validation.collector.ValidationCollector;
import com.documaster.validator.validation.collector.ValidationResult;
import com.documaster.validator.validation.utils.DefaultXMLHandler;
import com.documaster.validator.validation.utils.SchemaValidator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XMLReport<T extends Command<?> & ConfigurableReporting> extends Report<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLReport.class);

	private static final String DEFAULT_NAMESPACE = "http://documaster.com/schema/noark-extraction-validator/xml-report";

	private static final String SCHEMA_FILE_LOCATION = "reporting/xml-report.xsd";

	private XMLStreamWriter writer;

	XMLReport(T config, ValidationCollector collector, String title) {

		super(config, collector, title);
	}

	@Override
	void generate() throws IOException, XMLStreamException {

		File report = new File(getConfig().getReportConfiguration().getOutputDir(), getDefaultReportName() + ".xml");

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

		try {
			writer = outputFactory.createXMLStreamWriter(new FileWriter(report));

			writer.writeStartDocument();
			start("validation");

			writer.writeDefaultNamespace(DEFAULT_NAMESPACE);
			attr("type", getConfig().getName());

			writeExecutionInformation();
			writeValidationSummary();
			writeValidationDetails();

			end();
			writer.flush();

		} finally {
			if (writer != null) {
				writer.close();
			}
		}

		File schema = copyXSDSchema();

		validateReport(report, schema);
	}

	private void writeExecutionInformation() throws XMLStreamException {

		start("execution");

		// General information
		for (Map.Entry<String, Object> generalInfo : getConfig().getExecutionInfo().getGeneralInfo().entrySet()) {
			String key = !StringUtils.isBlank(generalInfo.getKey()) ? generalInfo.getKey() : "-";
			String value = generalInfo.getValue() != null ? generalInfo.getValue().toString() : "-";
			attr(key, value);
		}

		// Parameter information
		for (Command.ParameterInfo param : getConfig().getExecutionInfo().getParameterInfo()) {
			if (param.isDefault()) {
				// Skip default parameters
				continue;
			}

			start("parameter");

			attr("name", param.getName());
			attr("value", param.getSpecifiedValue().toString());
			attr("description", param.getDescription());
			attr("required", Boolean.toString(param.isRequired()));

			// End parameter
			end();
		}

		// Command-specific (other) information
		for (Map.Entry<String, String> commandInfo : getConfig().getExecutionInfo().getCommandInfo().entrySet()) {
			start("other");
			attr("name", commandInfo.getKey());
			cdataContent(commandInfo.getValue());
			end();
		}

		// End section
		end();
	}

	private void writeValidationSummary() throws XMLStreamException {

		start("summary");
		attr("summary", getCollector().getTotalSummaryCount());
		attr("info", getCollector().getTotalInformationCount());
		attr("warn", getCollector().getTotalWarningCount());
		attr("error", getCollector().getTotalErrorCount());

		for (Map.Entry<String, List<ValidationResult>> group : getCollector().getAllResults().entrySet()) {

			startGroup(group.getKey());

			for (ValidationResult test : group.getValue()) {
				startTest(test);
				end();
			}
			// End group
			end();
		}
		// End section
		end();
	}

	private void writeValidationDetails() throws XMLStreamException {

		start("details");

		for (Map.Entry<String, List<ValidationResult>> group : getCollector().getAllResults().entrySet()) {

			startGroup(group.getKey());

			for (ValidationResult test : group.getValue()) {

				startTest(test);

				testDetails("summary", test.getSummary());
				testDetails("info", test.getInformation());
				testDetails("warn", test.getWarnings());
				testDetails("error", test.getErrors());

				end();
			}
			// End group
			end();
		}
		// End section
		end();
	}

	private File copyXSDSchema() throws IOException {

		File schema = new File(getConfig().getReportConfiguration().getOutputDir(), getDefaultReportName() + ".xsd");

		FileUtils.copyInputStreamToFile(getClass().getClassLoader().getResourceAsStream(SCHEMA_FILE_LOCATION), schema);

		return schema;
	}

	private void validateReport(File report, File schema) {

		SchemaValidator schemaValidator = new SchemaValidator<>(new DefaultXMLHandler());

		if (!schemaValidator.isXmlFileValid(report, Collections.singletonList(schema))) {
			LOGGER.warn("The generated XML report does not validate against the bundled schema:");
			for (BaseItem error : schemaValidator.getHandler().getExceptionsAsItems()) {
				LOGGER.warn(error.toString());
			}
		}
	}

	private void start(String name) throws XMLStreamException {

		writer.writeStartElement(name);
	}

	private void startGroup(String groupName) throws XMLStreamException {

		start("group");

		attr("name", groupName);
		attr("summary", getCollector().getSummaryCountIn(groupName));
		attr("info", getCollector().getInformationCountIn(groupName));
		attr("warn", getCollector().getWarningCountIn(groupName));
		attr("error", getCollector().getErrorCountIn(groupName));
	}

	private void startTest(ValidationResult test) throws XMLStreamException {

		start("test");

		attr("id", test.getId());
		attr("name", test.getTitle());
		attr("description", test.getDescription());
		attr("summary", test.getSummary().size());
		attr("info", test.getInformation().size());
		attr("warn", test.getWarnings().size());
		attr("error", test.getErrors().size());
	}

	private void testDetails(String type, List<BaseItem> items) throws XMLStreamException {

		start(type);
		attr("total", items.size());

		for (BaseItem item : items) {

			start("row");

			for (Map.Entry<String, Object> col : item.getValues().entrySet()) {
				start("col");
				attr("name", col.getKey());
				content(col.getValue());
				end();
			}
			// End row
			end();
		}
		// End test details
		end();
	}

	private void attr(String name, int value) throws XMLStreamException {

		writer.writeAttribute(name, Integer.toString(value));
	}

	private void attr(String name, String value) throws XMLStreamException {

		writer.writeAttribute(name, value != null ? value : "-");
	}

	private void content(Object data) throws XMLStreamException {

		writer.writeCharacters(data != null ? data.toString() : "-");
	}

	private void cdataContent(Object data) throws XMLStreamException {

		writer.writeCData(data != null ? data.toString() : "-");
	}

	private void end() throws XMLStreamException {

		writer.writeEndElement();
	}
}
