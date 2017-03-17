package com.documaster.validator.exceptions.aggregation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.xml.sax.SAXParseException;

public class SAXParseExceptionAggregator<T extends SAXParseException> implements ExceptionAggregator<T> {

	/**
	 * Non-greedy regex matching the first occurrence of any value in single quotes.
	 */
	private final static String VALUE_MATCHING_REGEX = "'.*?'";
	private static final String DEFAULT_REPLACE_STRING = "X";

	private String replaceString = DEFAULT_REPLACE_STRING;

	/**
	 * Get the current {@code replaceString}.
	 */
	public String getReplaceString() {

		return replaceString;
	}

	/**
	 * Change the default {@code replaceString} ({@link SAXParseExceptionAggregator#DEFAULT_REPLACE_STRING}).
	 */
	public void setReplaceString(String replaceString) {

		this.replaceString = replaceString;
	}

	/**
	 * Aggregates the {@link SAXParseException}s based on their {@link SAXParseException#getMessage()} values and
	 * returns the number of occurrences of each.
	 * <p/>
	 * Note that some parts of the aggregated exception messages may be modified when added to the result set
	 * in order to ensure true aggregation. Such <i>parts</i> may include any non-deterministic information
	 * provided by the {@link SAXParseException}s.
	 * <p/>
	 * For example, the following exception messages:
	 * <pre>
	 * cvc-datatype-valid.1.2.1: '2017-01-01' is not a valid value for 'dateTime'
	 * cvc-datatype-valid.1.2.1: '2017-01-02' is not a valid value for 'dateTime'
	 * cvc-datatype-valid.1.2.1: '2017-01-03' is not a valid value for 'dateTime'
	 * </pre>
	 * will have their <i>arbitrary values</i> ('2017-01-0[1,2,3]') replaced by the {@code replaceString}.
	 * As a result, the three exception messages will be grouped together to produce the following output
	 * (assuming that the {@code replaceString} is <b>X</b>):
	 * <pre>
	 * {cvc-datatype-valid.1.2.1: X is not a valid value for 'dateTime': 3}
	 * </pre>
	 * The logic in this method relies on the fact that the Xerces XML parser is used. The list of exception message
	 * definitions can be found in {@code org.apache.xerces.impl.msg.XMLSchemaMessages}.
	 *
	 * @param exceptions
	 * 		A {@link List} of {@link SAXParseException}s to aggregate
	 * @return A {@link Map} whose keys are the (modified) exception messages and whose values are the number of times
	 * they occur
	 */
	@Override
	public Map<String, Long> aggregate(List<T> exceptions) {

		return exceptions.stream()
				.map(ex -> {
					String msg = ex.getMessage();
					switch (msg.substring(0, msg.indexOf(":"))) {
						case "cvc-attribute.3":
						case "cvc-attribute.4":
						case "cvc-complex-type.3.1":
						case "cvc-complex-type.3.2.2":
						case "cvc-datatype-valid.1.2.1":
						case "cvc-datatype-valid.1.2.2":
						case "cvc-datatype-valid.1.2.3":
						case "cvc-elt.4.1":
						case "cvc-elt.4.2":
						case "cvc-elt.4.3":
						case "cvc-elt.5.1.1":
						case "cvc-elt.5.2.2.2.1":
						case "cvc-elt.5.2.2.2.2":
						case "cvc-enumeration-valid":
						case "cvc-maxExclusive-valid":
						case "cvc-maxInclusive-valid":
						case "cvc-minExclusive-valid":
						case "cvc-minInclusive-valid":
						case "cvc-pattern-valid":
						case "cvc-type.3.1.3":
							return msg.replaceFirst(VALUE_MATCHING_REGEX, replaceString);
						case "cvc-fractionDigits-valid":
						case "cvc-length-valid":
						case "cvc-maxLength-valid":
						case "cvc-minLength-valid":
						case "cvc-totalDigits-valid":
							return msg.replaceFirst(VALUE_MATCHING_REGEX, replaceString)
									.replaceFirst(VALUE_MATCHING_REGEX, replaceString);
						default:
							// Keep all other messages the way they are
							return msg;
					}
				})
				.collect(Collectors.groupingBy(m -> m, Collectors.counting()));
	}
}
