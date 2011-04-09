package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import __TOP_LEVEL_PACKAGE__.client.managed.ui.ApplicationListPlaceRenderer;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.ProxyListPlace;
import __TOP_LEVEL_PACKAGE__.client.scaffold.ui.LoginWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;

/**
 * The outermost UI of the application.
 */
public class ScaffoldDesktopShell extends Composite {
	
	interface Binder extends UiBinder<Widget, ScaffoldDesktopShell> {
	}

	private static final Binder BINDER = GWT.create(Binder.class);

	@UiField SimplePanel details;
	@UiField DivElement error;
	@UiField LoginWidget loginWidget;
	@UiField SimplePanel master;
	@UiField NotificationMole mole;
	@UiField(provided = true)
	ValuePicker<ProxyListPlace> placesBox = new ValuePicker<ProxyListPlace>(new ApplicationListPlaceRenderer());

	public ScaffoldDesktopShell() {
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
