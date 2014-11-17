package org.springframework.roo.addon.web.mvc.jsp.i18n.languages;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.AbstractLanguage;
import org.springframework.roo.support.util.FileUtils;

/**
 * Dutch language support.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class DuchLanguage extends AbstractLanguage {

    public InputStream getFlagGraphic() {
        return FileUtils.getInputStream(getClass(), "nl.png");
    }

    public String getLanguage() {
        return "Dutch";
    }

    public Locale getLocale() {
        return new Locale("nl");
    }

    public InputStream getMessageBundle() {
        return FileUtils.getInputStream(getClass(), "messages_nl.properties");
    }
}
