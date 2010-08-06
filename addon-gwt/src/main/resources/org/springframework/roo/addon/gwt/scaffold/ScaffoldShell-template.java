package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.client.NotificationMole;
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
 * The outermost UI of the application.
 */
public class ScaffoldShell extends Composite {
	interface Binder extends UiBinder<Widget, ScaffoldShell> {
	}

	private static final Binder BINDER = GWT.create(Binder.class);

	@UiField SimplePanel details;
	@UiField DivElement error;
	@UiField LoginWidget loginWidget;
	@UiField SimplePanel master;
	@UiField NotificationMole mole;
	@UiField PlacePickerView<ApplicationListPlace> placesBox;

	public ScaffoldShell() {
		initWidget(BINDER.createAndBindUi(this));
	}

	/**
	 * @return the panel to hold the details
	 */
	public SimplePanel getDetailsPanel() {
		return details;
	}

	/**
	 * @return the login widget
	 */
	public LoginWidget getLoginWidget() {
		return loginWidget;
	}

	/**
	 * @return the panel to hold the master list
	 */
	public SimplePanel getMasterPanel() {
		return master;
	}

	/**
	 * @return the notification mole for loading feedback
	 */
	public NotificationMole getMole() {
		return mole;
	}

	/**
	 * @return the navigator
	 */
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
