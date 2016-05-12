package org.springframework.roo.addon.web.mvc.i18n.components;

import java.util.Locale;

/**
 * Convenience class for I18n implementations. Offers equals and hashCode method
 * implementations based on Locale (only!). Offers also toString().
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public abstract class AbstractLanguage implements I18n {

  private String localePrefix;
  private String language;

  public AbstractLanguage() {
    this.localePrefix = this.getLocale().getLanguage();
    this.language = this.getLanguage();
  }

  /**
   * equals compares locale only!
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof I18n)) {
      return false;
    }

    final Locale thisLocale = getLocale();
    final Locale other = ((I18n) obj).getLocale();
    if (thisLocale == null) {
      if (other != null) {
        return false;
      }
    } else if (!thisLocale.equals(other)) {
      return false;
    }
    return true;
  }

  /**
   * hashCode uses locale only!
   */
  @Override
  public int hashCode() {
    final Locale locale = getLocale();
    final int prime = 31;
    int result = 1;
    result = prime * result + (locale == null ? 0 : locale.hashCode());
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Locale: ").append(getLocale());
    sb.append("Language label: ").append(getLanguage());
    return sb.toString();
  }

  public String getLocalePrefix() {
    return localePrefix;
  }


  public void setLocalePrefix(String localePrefix) {
    this.localePrefix = localePrefix;
  }


  public String getLanguage() {
    return language;
  }


  public void setLanguage(String language) {
    this.language = language;
  }

}
