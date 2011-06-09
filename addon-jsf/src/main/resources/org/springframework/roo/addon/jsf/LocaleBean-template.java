package __PACKAGE__;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

@ManagedBean
@SessionScoped
public class LocaleBean {
	private Locale locale;

	public LocaleBean() {
		locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
	}

	public Locale getLocale() {
		return this.locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public SelectItem[] getLocales() {
		List<SelectItem> items = new ArrayList<SelectItem>();
		Application application = FacesContext.getCurrentInstance().getApplication();
		Iterator<Locale> supportedLocales = application.getSupportedLocales();
		while (supportedLocales.hasNext()) {
			Locale locale = supportedLocales.next();
			items.add(new SelectItem(locale.toString(), locale.getDisplayName()));
		}
		SelectItem[] locales = new SelectItem[items.size()];
		items.toArray(locales);
		return locales;
	}

	public String getSelectedLocale() {
		return getLocale().toString();
	}

	public void setSelectedLocale() {
		String localeString = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("locale");
		locale = new Locale(localeString);
	}
}
