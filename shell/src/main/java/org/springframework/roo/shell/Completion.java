package org.springframework.roo.shell;

import org.springframework.roo.support.util.AnsiEscapeCode;
import org.springframework.roo.support.util.StringUtils;

public class Completion {

	private String value;
	private String formattedValue;
	private String heading;
	private int order;

	public Completion(String value) {
		this(value, value, null, 0);
	}

	public Completion(String value, String formattedValue, String heading, int order) {
		this.value = value;
		this.formattedValue = formattedValue;
		if (StringUtils.hasText(heading)) {
			heading = AnsiEscapeCode.decorate(heading, AnsiEscapeCode.UNDERSCORE, AnsiEscapeCode.FG_GREEN);
		}
		this.heading = heading;
		this.order = order;
	}

	public String getValue() {
		return value;
	}

	public String getFormattedValue() {
		return formattedValue;
	}

	public String getHeading() {
		return heading;
	}

	public int getOrder() {
		return order;
	}

	@Override
	public String toString() {
		return order + ". " + heading + " - " + value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Completion that = (Completion) o;

		if (formattedValue != null ? !formattedValue.equals(that.formattedValue) : that.formattedValue != null)
			return false;
		if (heading != null ? !heading.equals(that.heading) : that.heading != null) return false;
		if (value != null ? !value.equals(that.value) : that.value != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = value != null ? value.hashCode() : 0;
		result = 31 * result + (formattedValue != null ? formattedValue.hashCode() : 0);
		result = 31 * result + (heading != null ? heading.hashCode() : 0);
		return result;
	}
}

