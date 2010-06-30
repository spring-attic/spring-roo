package org.springframework.roo.addon.web.mvc.jsp.i18n;

import java.io.InputStream;
import java.util.Locale;

/**
 * This interface is needs to be implemented by translation providers for the
 * Roo MVC JSP scaffolded UI. 
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
public interface I18n {
	
	/**
	 * The locale can be initialized statically or by using the constructor if the 
	 * langauge is not statically supported or if a country specific language translation 
	 * is provided (ie en_AU):
	 * 
	 * static:
	 * Locale.ENGLISH
	 * 
	 * constructor (no country):
	 * new Locale("en"); //lowercase two-letter ISO-639 code.
	 * 
	 * constructor (country specific):
	 * new Locale("en", "AU"); //language lowercase two-letter ISO-639 code, country uppercase two-letter ISO-3166 code.
	 *
	 * @return the locale
	 */
	Locale getLocale();
	
	/**
	 * The language label to be presented in the Web UI (ie: "English")
	 * 
	 * @return the language
	 */
	String getLanguage();
	
	/**
	 * The input stream for the flag graphic (must be a png image 16 x 11 pixels, 72 DPI). 
	 * Preferred flag icon set is the Fam Fam Fam set at http://www.famfamfam.com/lab/icons/flags/
	 * 
	 * @return the flag image stream
	 */
	InputStream getFlagGraphic();
	
	/**
	 * The input stream for the translated message bundle. It will be saved in the addon
	 * according to the locale provided (ie messages_en.properties, or messages_en_AU.properties)
	 * 
	 * @return the message bundle input stream
	 */
	InputStream getMessageBundle();
}
