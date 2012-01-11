package org.springframework.roo.addon.web.mvc.jsp.i18n.languages;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.jsp.i18n.AbstractLanguage;
import org.springframework.roo.support.util.FileUtils;

/**
 * Svedish language support.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class SwedishLanguage extends AbstractLanguage {

    public InputStream getFlagGraphic() {
        return FileUtils.getInputStream(getClass(), "sv.png");
    }

    public String getLanguage() {
        return "Svenska";
    }

    public Locale getLocale() {
        return new Locale("sv");
    }

    public InputStream getMessageBundle() {
        return FileUtils.getInputStream(getClass(), "messages_sv.properties");
    }
}
