package org.springframework.roo.addon.web.mvc.jsp.i18n;

import java.util.List;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link I18n}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class I18nConverter implements Converter<I18n> {

    @Reference private I18nSupport i18nSupport;

    public I18n convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (value.length() == 2) {
            return i18nSupport.getLanguage(new Locale(value, "", ""));
            // Disabled due to ROO-1584
            // } else if (value.length() == 5) {
            // String[] split = value.split("_");
            // return i18nSupport.getLanguage(new Locale(split[0],
            // split[1].toUpperCase(), ""));
        }
        return null;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        for (final I18n i18n : i18nSupport.getSupportedLanguages()) {
            final Locale locale = i18n.getLocale();
            final StringBuilder localeString = new StringBuilder(
                    locale.getLanguage());
            if (locale.getCountry() == null || locale.getCountry().length() > 0) {
                localeString.append("_").append(
                        locale.getCountry().toUpperCase());
            }
            completions.add(new Completion(localeString.toString()));
        }
        return true;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return I18n.class.isAssignableFrom(requiredType);
    }
}
