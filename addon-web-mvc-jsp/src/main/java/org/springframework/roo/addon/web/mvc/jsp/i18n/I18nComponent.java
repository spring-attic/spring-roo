package org.springframework.roo.addon.web.mvc.jsp.i18n;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;

/**
 * Listener for OSGi service events for registering and unregistering
 * I18n addons.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component(immediate=true)
@Service
@Reference(name="language", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=I18n.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
public class I18nComponent implements I18nSupport {
	
	private Object mutex = this;
	
	private Set<I18n> i18nSet = new HashSet<I18n>();
	
	protected void bindLanguage(I18n i18n) {
		synchronized (mutex) {
			i18nSet.add(i18n);
		}
	}
	
	protected void unbindLanguage(I18n i18n) {
		synchronized (mutex) {
			i18nSet.remove(i18n);
		}
	}

	public Set<I18n> getSupportedLanguages() {
		Set<I18n> set = null;
		synchronized (mutex) {
			set = Collections.unmodifiableSet(i18nSet);
		}
		return set;
	}
	
	public I18n getLanguage(Locale locale) {
		synchronized (mutex) {
			for (I18n lang: Collections.unmodifiableSet(i18nSet)) {
				if (lang.getLocale().toString().equalsIgnoreCase(locale.toString())) {
					return lang;
				}
			}
		}
		return null;
	}
}
