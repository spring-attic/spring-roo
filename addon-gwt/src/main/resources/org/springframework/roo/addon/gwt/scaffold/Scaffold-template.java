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

    /* Left side lets us pick from all the types of entities */

    PlacePicker<ApplicationListPlace> placePicker = new PlacePicker<ApplicationListPlace>(
        shell.getPlacesBox(), placeController, new ListPlaceRenderer());
    placePicker.setPlaces(getTopPlaces());

    /*
     * The body is run by an ActivitManager that listens for PlaceChange events
     * and finds the corresponding Activity to run
     */

    final ActivityMapper<ApplicationPlace> mapper = new ScaffoldActivities(
        requestFactory, placeController);
    final ActivityManager<ApplicationPlace> activityManager = new ActivityManager<ApplicationPlace>(
        mapper, eventBus);

    activityManager.setDisplay(new Activity.Display() {
      public void showActivityWidget(IsWidget widget) {
        shell.getBody().setWidget(widget == null ? null : widget.asWidget());
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
    ApplicationEntityTypesProcessor.processAll(new ApplicationEntityTypesProcessor.
        EntityTypesProcessor() {
      public void processType(Class<? extends Record> recordType) {
        rtn.add(new ApplicationListPlace(recordType));
      }
    });
    return Collections.unmodifiableList(rtn);
  }
}

