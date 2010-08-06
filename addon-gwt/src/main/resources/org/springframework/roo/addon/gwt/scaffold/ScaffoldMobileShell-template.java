package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.PlacePickerView;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.requestfactory.client.LoginWidget;
import __TOP_LEVEL_PACKAGE__.gwt.scaffold.place.ApplicationListPlace;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * TODO
 */
public class ScaffoldMobileShell extends Composite {
	interface Binder extends UiBinder<Widget, ScaffoldMobileShell> {
	}

	private static final Binder BINDER = GWT.create(Binder.class);

	@UiField SimplePanel body;
	@UiField DivElement error;
	@UiField LoginWidget loginWidget;
	@UiField PlacePickerView<ApplicationListPlace> placesBox;

	public ScaffoldMobileShell() {
		initWidget(BINDER.createAndBindUi(this));
	}

	/**
	 * @return the body
	 */
	public SimplePanel getBody() {
		return body;
	}

	/**
	 * @return the login widget
	 */
	public LoginWidget getLoginWidget() {
	  return loginWidget;
	}

	public PlacePickerView<ApplicationListPlace> getPlacesBox() {
		return placesBox;
	}

	/**
	 * @param string
	 */
	public void setError(String string) {
		error.setInnerText(string);
	}
}
