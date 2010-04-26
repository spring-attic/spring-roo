package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.app.place.PlaceChanged;
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
import __TOP_LEVEL_PACKAGE__.gwt.place.ApplicationListPlace;
import __TOP_LEVEL_PACKAGE__.gwt.place.ApplicationPlace;
import __TOP_LEVEL_PACKAGE__.gwt.place.ApplicationPlaces;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationEntityTypesProcessor;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationRequestFactory;
import __TOP_LEVEL_PACKAGE__.gwt.ui.ApplicationKeyNameRenderer;
import __TOP_LEVEL_PACKAGE__.gwt.ui.ListPlaceRenderer;

/**
 * Application for browsing the entities of the application.
 */
public class Scaffold implements EntryPoint {

  public void onModuleLoad() {

    // App controllers and services
    final HandlerManager eventBus = new HandlerManager(null);
    final ApplicationRequestFactory requestFactory = GWT.create(ApplicationRequestFactory.class);
    requestFactory.init(eventBus);

    final PlaceController<ApplicationPlace> placeController = new PlaceController<ApplicationPlace>(eventBus);
    final ApplicationPlaces places = new ApplicationPlaces(placeController);

    // Renderers
    final ApplicationKeyNameRenderer entityNamer = new ApplicationKeyNameRenderer();
    final ListPlaceRenderer listPlaceNamer = new ListPlaceRenderer();

    // Top level UI
    final ScaffoldShell shell = new ScaffoldShell();

    // Left side
    PlacePicker<ApplicationListPlace> placePicker = new PlacePicker<ApplicationListPlace>(shell.getPlacesBox(), placeController, listPlaceNamer);
    final List<ApplicationListPlace> topPlaces = new ArrayList<ApplicationListPlace>();
    ApplicationEntityTypesProcessor.processAll(new ApplicationEntityTypesProcessor.EntityTypesProcessor() {
		@Override
		public void processRecord(Class<? extends Record> recordType) {
			topPlaces.add(new ApplicationListPlace(recordType));
		}
	});
    placePicker.setPlaces(Collections.unmodifiableList(topPlaces));

    // Shows entity lists
    eventBus.addHandler(PlaceChanged.TYPE, new ScaffoldListRequester(places, shell.getBody(), requestFactory, listPlaceNamer));

    // Shared view for entity details.
    // TODO Real app should not share
    final HTML detailsView = new HTML();
    eventBus.addHandler(PlaceChanged.TYPE, new ScaffoldDetailsRequester(entityNamer, shell.getBody(), detailsView));

    // Hide the loading message
    Element loading = Document.get().getElementById("loading");
    loading.getParentElement().removeChild(loading);

    RootLayoutPanel.get().add(shell);
  }
}
