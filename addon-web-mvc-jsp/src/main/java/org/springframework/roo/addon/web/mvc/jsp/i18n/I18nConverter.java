package org.springframework.roo.addon.web.mvc.jsp.i18n;

import java.util.List;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link I18n}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@Component(immediate=true)
@Service
public class I18nConverter implements Converter {
	
	@Reference private I18nSupport i18nSupport;

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value.length() == 2) {
			return i18nSupport.getLanguage(new Locale(value,"","")); 
			// disabled due to ROO-1584
//		} else if (value.length() == 5) {
//			String[] split = value.split("_");
//			return i18nSupport.getLanguage(new Locale(split[0], split[1].toUpperCase(), ""));
		} else {
			return null;
		}
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		for(I18n i18n: i18nSupport.getSupportedLanguages()) {
			Locale locale = i18n.getLocale();
			StringBuffer localeString = new StringBuffer(locale.getLanguage());
			if (locale.getCountry() == null || locale.getCountry().length() > 0) {
				localeString.append("_").append(locale.getCountry().toUpperCase());
			}
			completions.add(localeString.toString());
		}
		return true;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return I18n.class.isAssignableFrom(requiredType);
	}

}
