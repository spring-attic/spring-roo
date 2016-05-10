package org.springframework.roo.addon.i18n.languages;

import java.io.InputStream;
import java.util.Locale;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.i18n.components.AbstractLanguage;
import org.springframework.roo.support.util.FileUtils;

/**
 * Brazilian portuguese language support.
 * 
 * @author Levi Soares
 * @since 2.0
 */
@Component
@Service
public class PortuguesBRLanguage extends AbstractLanguage {

  @Override
  public InputStream getFlagGraphic() {
    return FileUtils.getInputStream(getClass(), "ptbr.png");
  }

  @Override
  public String getLanguage() {
    return "Portugues";
  }

  @Override
  public Locale getLocale() {
    return new Locale("pt");
  }

  @Override
  public InputStream getMessageBundle() {
    return FileUtils.getInputStream(getClass(), "messages_ptbr.properties");
  }

}
