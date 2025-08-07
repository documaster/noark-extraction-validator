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
package com.documaster.validator.validation.noark5.provider;

import java.util.Collections;
import java.util.List;

import com.documaster.validator.validation.noark5.provider.rules.Check;
import com.documaster.validator.validation.noark5.provider.rules.Test;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "validation")
public class ValidationProvider {

	@XmlElement(required = true, name = "target")
	protected List<String> targets;

	@XmlElement(required = true, name = "test")
	protected List<Test> tests;

	@XmlElement(required = true, name = "check")
	protected List<Check> checks;

	public List<String> getTargets() {

		return targets;
	}

	public List<Test> getTests() {

		if (tests == null) {
			return Collections.emptyList();
		}

		return this.tests;
	}

	public List<Check> getChecks() {

		if (checks == null) {
			return Collections.emptyList();
		}

		return this.checks;
	}

}
