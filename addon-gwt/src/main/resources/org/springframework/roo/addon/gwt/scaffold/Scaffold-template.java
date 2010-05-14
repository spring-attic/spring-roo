package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityManager;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.app.place.PlaceChangeEvent;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.PlacePicker;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.RequestEvent.State;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.app.util.IsWidget;

import __TOP_LEVEL_PACKAGE__.gwt.scaffold.place.ApplicationListPlace;
import __TOP_LEVEL_PACKAGE__.gwt.scaffold.place.ApplicationPlace;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationEntityTypesProcessor;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationRequestFactory;
import __TOP_LEVEL_PACKAGE__.gwt.ui.ApplicationKeyNameRenderer;
import __TOP_LEVEL_PACKAGE__.gwt.ui.ListPlaceRenderer;
import __TOP_LEVEL_PACKAGE__.gwt.ui.ListActivitiesMapper;


/**
 * Application for browsing the entities of the Expenses app.
 */
public class Scaffold implements EntryPoint {

  public void onModuleLoad() {

    /* App controllers and services */

    final HandlerManager eventBus = new HandlerManager(null);
    final ApplicationRequestFactory requestFactory = GWT.create(ApplicationRequestFactory.class);
    requestFactory.init(eventBus);
    final PlaceController<ApplicationPlace> placeController = new PlaceController<ApplicationPlace>(
        eventBus);

    /* Top level UI */

    final ScaffoldShell shell = new ScaffoldShell();

    /* Display loading notifications when we touch the network. */

    eventBus.addHandler(RequestEvent.TYPE, new RequestEvent.Handler() {
      // Only show loading status if a request isn't serviced in 250ms.
      private static final int LOADING_TIMEOUT = 250;

      public void onRequestEvent(RequestEvent requestEvent) {
	if (requestEvent.getState() == State.SENT) {
	  shell.getMole().showDelayed(LOADING_TIMEOUT);
	} else {
	  shell.getMole().hide();
	}
      }
    });

    /* Left side lets us pick from all the types of entities */

    PlacePicker<ApplicationListPlace> placePicker = new PlacePicker<ApplicationListPlace>(
        shell.getPlacesBox(), placeController, new ListPlaceRenderer());
    placePicker.setPlaces(getTopPlaces());

    /*
     * The app is run by ActivityManager instances that listen for place change
     * events and run the appropriate Activity
     * 
     * The top half runs list activities of a traditional master / details view,
     * although here "master" is a misnomer. The two ActivityManagers are
     * completely independent of one another.
     */
    final ActivityManager<ApplicationPlace> masterActivityManager = new ActivityManager<ApplicationPlace>(
        new ScaffoldMasterActivities(new ListActivitiesMapper(eventBus,
            requestFactory, placeController)), eventBus);


    masterActivityManager.setDisplay(new Activity.Display() {
      public void showActivityWidget(IsWidget widget) {
        shell.getMasterPanel().setWidget(
            widget == null ? null : widget.asWidget());
      }
    });

    /*
     * The bottom half handles details
     */
    final ActivityManager<ApplicationPlace> detailsActivityManager = new ActivityManager<ApplicationPlace>(
        new ScaffoldDetailsActivities(requestFactory, placeController),
        eventBus);

    detailsActivityManager.setDisplay(new Activity.Display() {
      public void showActivityWidget(IsWidget widget) {
        shell.getDetailsPanel().setWidget(
            widget == null ? null : widget.asWidget());
      }
    });

    /* Hide the loading message */

    Element loading = Document.get().getElementById("loading");
    loading.getParentElement().removeChild(loading);

    /* And show the user the shell */

    RootLayoutPanel.get().add(shell);
  }

  private List<ApplicationListPlace> getTopPlaces() {
    final List<ApplicationListPlace> rtn = new ArrayList<ApplicationListPlace>();
    ApplicationEntityTypesProcessor.processAll(new ApplicationEntityTypesProcessor.EntityTypesProcessor() {
      public void processType(Class<? extends Record> recordType) {
        rtn.add(new ApplicationListPlace(recordType));
      }
    });
    return Collections.unmodifiableList(rtn);
  }
}

