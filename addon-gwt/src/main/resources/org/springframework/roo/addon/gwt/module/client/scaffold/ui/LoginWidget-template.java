package __TOP_LEVEL_PACKAGE__.client.scaffold.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import __TOP_LEVEL_PACKAGE__.shared.scaffold.UserInformationProxy;

/**
 * A simple widget which displays info about the user and a logout link.
 */
public class LoginWidget extends Composite {
  interface Binder extends UiBinder<Widget, LoginWidget> { }
  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField SpanElement name;
  String logoutUrl = "";
  
  public LoginWidget() {
    initWidget(BINDER.createAndBindUi(this));
  }
  
  /**
   * Sets the user information using a {@link UserInformationProxy}.
   *
   * @param info a {@link UserInformationProxy} instance
   */
  public void setUserInformation(UserInformationProxy info) {
    name.setInnerText(info.getName());
    logoutUrl = info.getLogoutUrl();
  }
  
  @UiHandler("logoutLink")
  void handleClick(@SuppressWarnings("unused") ClickEvent e) {
    if (logoutUrl != "") {
      Location.replace(logoutUrl);
    }
  }
}
