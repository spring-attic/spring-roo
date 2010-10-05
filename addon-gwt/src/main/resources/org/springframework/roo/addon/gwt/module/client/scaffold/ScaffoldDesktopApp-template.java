package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.gwt.app.place.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.requestfactory.client.AuthenticationFailureHandler;
import com.google.gwt.requestfactory.client.RequestFactoryLogHandler;
import com.google.gwt.requestfactory.shared.LoggingRequest;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.UserInformationProxy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            public void onSuccess(UserInformationProxy userInformationProxy) {
                shell.getLoginWidget().setUserInformation(userInformationProxy);
            }
        };
        requestFactory.userInformationRequest().getCurrentUserInformation(Window.Location.getHref()).fire(receiver);

        // Add remote logging handler
        RequestFactoryLogHandler.LoggingRequestProvider provider = new RequestFactoryLogHandler.LoggingRequestProvider() {
            public LoggingRequest getLoggingRequest() {
              return requestFactory.loggingRequest();
            }
        };
        Logger.getLogger("").addHandler(
            new RequestFactoryLogHandler(provider, Level.WARNING,
                                         new ArrayList<String>()));

        RequestEvent.register(eventBus, new RequestEvent.Handler() {
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

        RequestEvent.register(eventBus, new AuthenticationFailureHandler());

        CachingActivityMapper cached = new CachingActivityMapper(applicationMasterActivities);
        ProxyPlaceToListPlace proxyPlaceToListPlace = new ProxyPlaceToListPlace();
        ActivityMapper masterActivityMap = new FilteredActivityMapper(proxyPlaceToListPlace, cached);
        final ActivityManager masterActivityManager = new ActivityManager(masterActivityMap, eventBus);

        masterActivityManager.setDisplay(shell.getMasterPanel());

        ProxyListPlacePicker proxyListPlacePicker = new ProxyListPlacePicker(placeController, proxyPlaceToListPlace);
        HasConstrainedValue<ProxyListPlace> listPlacePickerView = shell.getPlacesBox();
        listPlacePickerView.setAcceptableValues(getTopPlaces());
        proxyListPlacePicker.register(eventBus, listPlacePickerView);

        final ActivityManager detailsActivityManager = new ActivityManager(applicationDetailsActivities, eventBus);

        detailsActivityManager.setDisplay(shell.getDetailsPanel());

        /* Browser history integration */
        ScaffoldPlaceHistoryMapper mapper = GWT.create(ScaffoldPlaceHistoryMapper.class);
        mapper.setFactory(placeHistoryFactory);
        PlaceHistoryHandler placeHistoryHandler = new PlaceHistoryHandler(mapper);
        ProxyListPlace defaultPlace = getTopPlaces().iterator().next();
        placeHistoryHandler.register(placeController, eventBus, defaultPlace);
        placeHistoryHandler.handleCurrentHistory();
    }
}
