package org.springframework.roo.addon.web.mvc.jsp.i18n.languages;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.AbstractLanguage;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Italian language support.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component(immediate=true)
@Service
public class ItalianLanguage extends AbstractLanguage {

	public Locale getLocale() {
		return Locale.ITALIAN;
	}
	
	public String getLanguage() {
		return "Italiano";
	}

	public InputStream getFlagGraphic() {
		return TemplateUtils.getTemplate(getClass(), "it.png");
	}

	public InputStream getMessageBundle() {
		return TemplateUtils.getTemplate(getClass(), "messages_it.properties");
	}
}
