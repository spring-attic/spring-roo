package org.springframework.roo.addon.web.mvc.jsp.i18n;

import java.util.Locale;
import java.util.Set;

/**
 * Service interface to allow addons to find all present I18n addons.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface I18nSupport {

    I18n getLanguage(Locale locale);

    Set<I18n> getSupportedLanguages();
}
