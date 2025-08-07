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
package com.documaster.validator.validation.noark5.provider.rules;

import com.documaster.validator.validation.noark5.provider.ValidationGroup;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import org.apache.commons.lang3.StringUtils;

public class Rule {

	@XmlAttribute(required = true, name = "id")
	protected String id;

	@XmlElement(required = true, name = "title")
	protected String title;

	@XmlElement(required = true, name = "description")
	protected String description;

	@XmlElement(required = true, name = "group")
	protected ValidationGroup group;

	public String getId() {

		return id;
	}

	public String getTitle() {

		return title;
	}

	public String getDescription() {

		return StringUtils.trimToEmpty(description).replaceAll("\\s+", " ");
	}

	public ValidationGroup getGroup() {

		return group;
	}
}
