package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import __TOP_LEVEL_PACKAGE__.client.scaffold.ui.LoginWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Top level UI for the mobile version of the application.
 */
public class ScaffoldMobileShell extends Composite {

	interface Binder extends UiBinder<Widget, ScaffoldMobileShell> {
	}

	private static final Binder BINDER = GWT.create(Binder.class);

	@UiField Button backButton;
	@UiField Element backButtonWrapper;
	@UiField SimplePanel body;
	@UiField Button editButton;
	@UiField LoginWidget loginWidget;
	@UiField Element title;

	public ScaffoldMobileShell() {
		initWidget(BINDER.createAndBindUi(this));
	}

	/**
	 * @return the back button
	 */
	public Button getBackButton() {
		return backButton;
	}

	/**
	 * @return the body
	 */
	public SimplePanel getBody() {
		return body;
	}

	/**
	 * @return the edit button
	 */
	public Button getEditButton() {
		return editButton;
	}

	/**
	 * @return the login widget
	 */
	public LoginWidget getLoginWidget() {
		return loginWidget;
	}

	/**
	 * Show or hide the back button.
	 *
	 * @param visible true to show the button, false to hide
	 */
	public void setBackButtonVisible(boolean visible) {
		setVisible(backButtonWrapper, visible);
	}

	/**
	 * Show or hide the edit button.
	 *
	 * @param visible true to show the button, false to hide
	 */
	public void setEditButtonVisible(boolean visible) {
		editButton.setVisible(visible);
	}

	/**
	 * Set the title of the app.
	 *
	 * @param text the title to display at the top of the app
	 */
	public void setTitleText(String text) {
		title.setInnerText(text);
	}
}
