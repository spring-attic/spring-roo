package org.springframework.roo.shell.converters;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;

/**
 * {@link Converter} for {@link Date}. By default the accepted DateFormat is "MM-dd-yyyy", however this can be changed by 
 * adding a bean definition to the roo-bootstrap.xml application context:
 * 
 * 	<bean class="org.springframework.roo.shell.converters.DateConverter">
 *   <constructor-arg>
 *    <bean class="java.text.SimpleDateFormat">
 *     <constructor-arg value="dd-MM-yyyy" />
 *    </bean>
 *   </constructor-arg>
 *  </bean>
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopment
public class DateConverter implements Converter {
	
	private DateFormat dateFormat;
	private String defaultPattern = "MM-dd-yyyy";
	
	public DateConverter() {
		this.dateFormat = new SimpleDateFormat(defaultPattern);
	}
	
	public DateConverter(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		try {
			return dateFormat.parse(value);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Could not parse date: " + e.getMessage());		
		}
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Date.class.isAssignableFrom(requiredType);
	}
}