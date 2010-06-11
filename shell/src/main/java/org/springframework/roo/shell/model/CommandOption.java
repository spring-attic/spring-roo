package org.springframework.roo.shell.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.shell.CliOption;

/**
 * Provides a simple representation of a {@link CliOption}.
 * 
 * <p>
 * Immutable once constructed. Intended for use within {@link CommandInfo} objects.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public class CommandOption {
	private boolean mandatory;
	private List<String> optionNames;
	private String help;
	private String specifiedDefaultValue;
	private String unspecifiedDefaultValue;
	
	public CommandOption(boolean mandatory, String specifiedDefaultValue, String unspecifiedDefaultValue, String help, String... optionNames) {
		if (optionNames == null || optionNames.length == 0) throw new IllegalStateException("Option names required");
		this.optionNames = Arrays.asList(optionNames);
		this.mandatory = mandatory;
		this.specifiedDefaultValue = specifiedDefaultValue;
		this.unspecifiedDefaultValue = unspecifiedDefaultValue;
		this.help = help;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public List<String> getOptionNames() {
		return Collections.unmodifiableList(optionNames);
	}

	public String getHelp() {
		return help;
	}

	public String getSpecifiedDefaultValue() {
		return specifiedDefaultValue;
	}

	public String getUnspecifiedDefaultValue() {
		return unspecifiedDefaultValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((help == null) ? 0 : help.hashCode());
		result = prime * result + (mandatory ? 1231 : 1237);
		result = prime * result
				+ ((optionNames == null) ? 0 : optionNames.hashCode());
		result = prime
				* result
				+ ((specifiedDefaultValue == null) ? 0 : specifiedDefaultValue
						.hashCode());
		result = prime
				* result
				+ ((unspecifiedDefaultValue == null) ? 0
						: unspecifiedDefaultValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommandOption other = (CommandOption) obj;
		if (help == null) {
			if (other.help != null)
				return false;
		} else if (!help.equals(other.help))
			return false;
		if (mandatory != other.mandatory)
			return false;
		if (optionNames == null) {
			if (other.optionNames != null)
				return false;
		} else if (!optionNames.equals(other.optionNames))
			return false;
		if (specifiedDefaultValue == null) {
			if (other.specifiedDefaultValue != null)
				return false;
		} else if (!specifiedDefaultValue.equals(other.specifiedDefaultValue))
			return false;
		if (unspecifiedDefaultValue == null) {
			if (other.unspecifiedDefaultValue != null)
				return false;
		} else if (!unspecifiedDefaultValue
				.equals(other.unspecifiedDefaultValue))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CommandOption [help=" + help + ", mandatory=" + mandatory
				+ ", optionNames=" + optionNames + ", specifiedDefaultValue="
				+ specifiedDefaultValue + ", unspecifiedDefaultValue="
				+ unspecifiedDefaultValue + "]";
	}
	
}