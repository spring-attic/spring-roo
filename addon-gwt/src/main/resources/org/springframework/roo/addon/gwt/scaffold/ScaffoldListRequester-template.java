package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.PlaceChanged;
import com.google.gwt.user.client.ui.Renderer;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.valuestore.client.ValuesListViewTable;
import __TOP_LEVEL_PACKAGE__.gwt.place.ApplicationListPlace;
import __TOP_LEVEL_PACKAGE__.gwt.place.ApplicationPlaces;
import __TOP_LEVEL_PACKAGE__.gwt.request.ApplicationRequestFactory;
import __TOP_LEVEL_PACKAGE__.gwt.scaffold.generated.ScaffoldListViewBuilder;

/**
 * In charge of requesting and displaying the appropriate record lists in the
 * appropriate view when the user goes to an {@link ApplicationListPlace} in the
 * Scaffold app.
 */
public final class ScaffoldListRequester implements PlaceChanged.Handler {

  private final SimplePanel panel;
  private final ScaffoldListViewBuilder scaffoldListViewBuilder;

  public ScaffoldListRequester(ApplicationPlaces places, SimplePanel panel, ApplicationRequestFactory requests, Renderer<ApplicationListPlace> renderer) {
    this.panel = panel;
    this.scaffoldListViewBuilder = new ScaffoldListViewBuilder(places, requests, renderer);
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof ApplicationListPlace)) {
      return;
    }
    final ApplicationListPlace newPlace = (ApplicationListPlace) event.getNewPlace();

    ValuesListViewTable<?> entitiesView = scaffoldListViewBuilder.getListView(newPlace);
    
    if (entitiesView == null) {
    	// We need to do one by hand
    	System.err.println("Unable to locate a scaffolded list view for " + newPlace);
    }
    
    if (entitiesView.getParent() == null) {
      panel.clear();
      panel.add(entitiesView);
    }
  }
}
