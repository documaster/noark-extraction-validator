package com.documaster.validator.exceptions.aggregation;

import java.util.List;
import java.util.Map;

public interface ExceptionAggregator<T extends Exception> {

	/**
	 * Aggregates the specified {@link List} of {@link T} exceptions by their message property and returns a {@link Map}
	 * holding the aggregated exception messages as keys and the number of their occurrences.
	 */
	Map<String, Long> aggregate(List<T> exceptions);
}
