package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.UserInformationProxy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasConstrainedValue;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

/**
 * Mobile application for browsing entities.
 * 
 * TODO (jgw): Make this actually mobile-friendly.
 */
public class ScaffoldMobileApp extends ScaffoldApp {
    private static final Logger log = Logger.getLogger(Scaffold.class.getName());

    private final ScaffoldMobileShell shell = new ScaffoldMobileShell();

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


        /* Left side lets us pick from all the types of entities */

        ProxyPlaceToListPlace proxyPlaceToListPlace = new ProxyPlaceToListPlace(requestFactory);
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
        ScaffoldPlaceHistoryHandler placeHistoryHandler = GWT.create(ScaffoldPlaceHistoryHandler.class);
        placeHistoryHandler.setFactory(placeHistoryFactory);
        placeHistoryHandler.register(placeController, eventBus, 
        		getTopPlaces().iterator().next()); /* defaultPlace */
        placeHistoryHandler.handleCurrentHistory();
    }

}
