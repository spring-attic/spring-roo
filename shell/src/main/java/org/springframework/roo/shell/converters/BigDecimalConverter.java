package org.springframework.roo.shell.converters;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link BigDecimal}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class BigDecimalConverter implements Converter<BigDecimal> {

	public BigDecimal convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new BigDecimal(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return BigDecimal.class.isAssignableFrom(requiredType);
	}

}