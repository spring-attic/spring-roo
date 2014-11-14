package org.springframework.roo.addon.web.mvc.jsp.i18n.languages;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.AbstractLanguage;
import org.springframework.roo.support.util.FileUtils;

/**
 * Spanish language support.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class SpanishLanguage extends AbstractLanguage {

    public InputStream getFlagGraphic() {
        return FileUtils.getInputStream(getClass(), "es.png");
    }

    public String getLanguage() {
        return "Espanol";
    }

    public Locale getLocale() {
        return new Locale("es");
    }

    public InputStream getMessageBundle() {
        return FileUtils.getInputStream(getClass(), "messages_es.properties");
    }
}
