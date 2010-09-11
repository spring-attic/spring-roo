package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.requestfactory.client.AuthenticationFailureHandler;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.UserInformationProxy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

/**
 * Application for browsing entities.
 */
public class ScaffoldDesktopApp extends ScaffoldApp {
    private static final Logger log = Logger.getLogger(Scaffold.class.getName());

    private final ScaffoldDesktopShell shell = new ScaffoldDesktopShell();

    public void run() {

        /* Add handlers, setup activities */

        init();

        /* Hide the loading message */

        Element loading = Document.get().getElementById("loading");
        loading.getParentElement().removeChild(loading);

        /* And show the user the shell */

        RootLayoutPanel.get().add(shell);

    }

    private void init() {

        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
	    public void onUncaughtException(Throwable e) {
	        Window.alert("Error: " + e.getMessage());
	        log.log(Level.SEVERE, e.getMessage(), e);
	    }
        });

        Receiver<UserInformationProxy> receiver = new Receiver<UserInformationProxy>() {
            public void onSuccess(UserInformationProxy userInformationProxy, Set<SyncResult> syncResults) {
                shell.getLoginWidget().setUserInformation(userInformationProxy);
            }
        };
        requestFactory.userInformationRequest().getCurrentUserInformation(Window.Location.getHref()).fire(receiver);

        eventBus.addHandler(RequestEvent.TYPE, new RequestEvent.Handler() {
            // Only show loading status if a request isn't serviced in 250ms.
            private static final int LOADING_TIMEOUT = 250;

            public void onRequestEvent(RequestEvent requestEvent) {
                if (requestEvent.getState() == RequestEvent.State.SENT) {
                    shell.getMole().showDelayed(LOADING_TIMEOUT);
                } else {
                    shell.getMole().hide();
                }
            }
        });

        /* Check for Authentication failures or mismatches */

        eventBus.addHandler(RequestEvent.TYPE, new AuthenticationFailureHandler());

        CachingActivityMapper cached = new CachingActivityMapper(applicationMasterActivities);
        ProxyPlaceToListPlace proxyPlaceToListPlace = new ProxyPlaceToListPlace(requestFactory);
        ActivityMapper masterActivityMap = new FilteredActivityMapper(proxyPlaceToListPlace, cached);
        final ActivityManager masterActivityManager = new ActivityManager(masterActivityMap, eventBus);

        masterActivityManager.setDisplay(new Activity.Display() {
            public void showActivityWidget(IsWidget widget) {
                shell.getMasterPanel().setWidget(widget == null ? null : widget.asWidget());
            }
        });

        ProxyListPlacePicker proxyListPlacePicker = new ProxyListPlacePicker(placeController, proxyPlaceToListPlace);
        HasConstrainedValue<ProxyListPlace> listPlacePickerView = shell.getPlacesBox();
        listPlacePickerView.setAcceptableValues(getTopPlaces());
        proxyListPlacePicker.register(eventBus, listPlacePickerView);

        final ActivityManager detailsActivityManager = new ActivityManager(applicationDetailsActivities, eventBus);

        detailsActivityManager.setDisplay(new Activity.Display() {
            public void showActivityWidget(IsWidget widget) {
                shell.getDetailsPanel().setWidget(widget == null ? null : widget.asWidget());
            }
        });


        ScaffoldPlaceHistoryHandler placeHistoryHandler = GWT.create(ScaffoldPlaceHistoryHandler.class);
        placeHistoryHandler.setFactory(placeHistoryFactory);
        placeHistoryHandler.register(placeController, eventBus,
	     getTopPlaces().iterator().next()); /* defaultPlace */
        placeHistoryHandler.handleCurrentHistory();
    }
}
