package org.springframework.roo.addon.web.mvc.jsp.i18n;

import java.util.Locale;

/**
 * Convenience class for I18n implementations. Offers equals and hashCode method
 * implementations based on Locale (only!). Offers also toString().
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public abstract class AbstractLanguage implements I18n {

	/**
	 * hashCode uses locale only!
	 */
	public int hashCode() {
		Locale locale = getLocale();
		final int prime = 31;
		int result = 1;
		result = prime * result + ((locale == null) ? 0 : locale.hashCode());
		return result;
	}
	
	/**
	 * equals compares locale only!
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof I18n))
			return false;
		
		Locale thisLocale = getLocale();
		Locale other = ((I18n) obj).getLocale();
		if (thisLocale == null) {
			if (other != null)
				return false;
		} else if (!thisLocale.equals(other))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Locale: ").append(getLocale());
		sb.append("Language label: ").append(getLanguage());
		return sb.toString();
	}
}
