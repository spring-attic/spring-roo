package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple widget which displays info about the user and a logout link.
 */
public class LoginWidget extends Composite {
	
	interface Binder extends UiBinder<Widget, LoginWidget> {
	}

	private static final Binder BINDER = GWT.create(Binder.class);

	@UiField SpanElement name;
	@UiField Anchor logoutLink;

	public LoginWidget() {
		initWidget(BINDER.createAndBindUi(this));
	}

	public void setUserName(String userName) {
		name.setInnerText(userName);
	}

	public void setLogoutUrl(String url) {
		logoutLink.setHref(url);
	}

	/**
	 * Squelch clicks of the logout link if no href has been set.
	 */
	@UiHandler("logoutLink")
	void handleClick(ClickEvent e) {
		if ("".equals(logoutLink.getHref())) {
			e.stopPropagation();
		}
	}
}
