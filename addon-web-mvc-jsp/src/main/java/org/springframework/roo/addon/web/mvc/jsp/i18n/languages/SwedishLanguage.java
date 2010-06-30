package org.springframework.roo.addon.web.mvc.jsp.i18n.languages;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.AbstractLanguage;
import org.springframework.roo.support.util.TemplateUtils;

/** 
 * Svedish language support.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component(immediate=true)
@Service
public class SwedishLanguage extends AbstractLanguage {

	public Locale getLocale() {
		return new Locale("sv");
	}
	
	public String getLanguage() {
		return "Svenska";
	}

	public InputStream getFlagGraphic() {
		return TemplateUtils.getTemplate(getClass(), "sv.png");
	}

	public InputStream getMessageBundle() {
		return TemplateUtils.getTemplate(getClass(), "messages_sv.properties");
	}
}
