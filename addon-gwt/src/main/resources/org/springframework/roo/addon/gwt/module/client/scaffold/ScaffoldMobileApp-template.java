package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.gwt.activity.shared.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.*;
import com.google.gwt.requestfactory.client.RequestFactoryLogHandler;
import com.google.gwt.requestfactory.shared.LoggingRequest;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.UserInformationProxy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;
import __TOP_LEVEL_PACKAGE__.client.request.ApplicationRequestFactory;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.ProxyListPlace;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.ProxyListPlacePicker;
import __TOP_LEVEL_PACKAGE__.client.scaffold.place.ProxyPlaceToListPlace;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mobile application for browsing entities.
 * 
 * TODO (jgw): Make this actually mobile-friendly.
 */
public class ScaffoldMobileApp extends ScaffoldApp {

    private static final Logger log = Logger.getLogger(Scaffold.class.getName());

    private final ScaffoldMobileShell shell;
    private final ScaffoldMobileActivities scaffoldMobileActivities;
    private final ApplicationRequestFactory requestFactory;
    private final EventBus eventBus;
    private final PlaceController placeController;
    private final PlaceHistoryFactory placeHistoryFactory;

    @Inject
    public ScaffoldMobileApp(ScaffoldMobileShell shell, ApplicationRequestFactory requestFactory, EventBus eventBus,
                               PlaceController placeController, ScaffoldMobileActivities scaffoldMobileActivities,
                               PlaceHistoryFactory placeHistoryFactory) {
        this.shell = shell;
        this.requestFactory = requestFactory;
        this.eventBus = eventBus;
        this.placeController = placeController;
        this.scaffoldMobileActivities = scaffoldMobileActivities;
        this.placeHistoryFactory = placeHistoryFactory;

    }

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

        /* Add remote logging handler */
        RequestFactoryLogHandler.LoggingRequestProvider provider = new RequestFactoryLogHandler.LoggingRequestProvider() {
            public LoggingRequest getLoggingRequest() {
              return requestFactory.loggingRequest();
            }
        };
        Logger.getLogger("").addHandler(
            new RequestFactoryLogHandler(provider, Level.WARNING,
                                         new ArrayList<String>()));

        /* Left side lets us pick from all the types of entities */

        ProxyPlaceToListPlace proxyPlaceToListPlace = new ProxyPlaceToListPlace();
        ProxyListPlacePicker proxyListPlacePicker = new ProxyListPlacePicker(placeController, proxyPlaceToListPlace);
        HasConstrainedValue<ProxyListPlace> placePickerView = shell.getPlacesBox();
        placePickerView.setAcceptableValues(getTopPlaces());
        proxyListPlacePicker.register(eventBus, placePickerView);

        /*
       * The body is run by an ActivityManager that listens for PlaceChange events and finds the corresponding Activity to run
       */

        final ActivityManager activityManager = new ActivityManager(scaffoldMobileActivities, eventBus);

        activityManager.setDisplay(shell.getBody());
        
        /* Browser history integration */
        ScaffoldPlaceHistoryMapper mapper = GWT.create(ScaffoldPlaceHistoryMapper.class);
        mapper.setFactory(placeHistoryFactory);
        PlaceHistoryHandler placeHistoryHandler = new PlaceHistoryHandler(mapper);
        ProxyListPlace defaultPlace = getTopPlaces().iterator().next();
        placeHistoryHandler.register(placeController, eventBus, defaultPlace);
        placeHistoryHandler.handleCurrentHistory();
    }

}
