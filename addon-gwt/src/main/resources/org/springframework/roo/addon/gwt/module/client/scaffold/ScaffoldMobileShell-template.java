package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.requestfactory.client.LoginWidget;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.ValuePicker;
import com.google.gwt.user.client.ui.Widget;

/**
 * Top level UI for the mobile version of the application.
 */
public class ScaffoldMobileShell extends Composite {

	interface Binder extends UiBinder<Widget, ScaffoldMobileShell> {
	}

	private static final Binder BINDER = GWT.create(Binder.class);

	@UiField SimplePanel body;
	@UiField DivElement error;
	@UiField(provided = true) ValuePicker<ProxyListPlace> placesBox = new ValuePicker<ProxyListPlace>(new ApplicationListPlaceRenderer());
	@UiField LoginWidget loginWidget;

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

	public HasConstrainedValue<ProxyListPlace> getPlacesBox() {
		return placesBox;
	}

	/**
	 * @param string
	 */
	public void setError(String string) {
		error.setInnerText(string);
	}
}
