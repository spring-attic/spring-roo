package org.springframework.roo.addon.web.mvc.jsp.i18n.languages;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.AbstractLanguage;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * English language support.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component(immediate=true)
@Service
public class EnglishLanguage extends AbstractLanguage {

	public Locale getLocale() {
		return Locale.ENGLISH;
	}
	
	public String getLanguage() {
		return "English";
	}

	public InputStream getFlagGraphic() {
		return TemplateUtils.getTemplate(getClass(), "gb.png");
	}

	public InputStream getMessageBundle() {
		return TemplateUtils.getTemplate(getClass(), "messages.properties");
	}
}
