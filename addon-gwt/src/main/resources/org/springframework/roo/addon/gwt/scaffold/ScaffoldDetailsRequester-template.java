package __TOP_LEVEL_PACKAGE__.gwt.scaffold;

import com.google.gwt.app.place.PlaceChanged;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.valuestore.shared.Record;
import __TOP_LEVEL_PACKAGE__.gwt.place.ApplicationDetailsPlace;
import __TOP_LEVEL_PACKAGE__.gwt.scaffold.generated.ScaffoldDetailsViewBuilder;
import __TOP_LEVEL_PACKAGE__.gwt.ui.ApplicationKeyNameRenderer;


/**
 * In charge of requesting and displaying details of a particular record in the
 * appropriate view when the user goes to an {@link ApplicationDetailsPlace} in the
 * Scaffold app.
 */
public final class ScaffoldDetailsRequester implements PlaceChanged.Handler {
  private final ApplicationKeyNameRenderer entityNamer;
  private final SimplePanel panel;
  private final HTML detailsView;

  public ScaffoldDetailsRequester(ApplicationKeyNameRenderer entityNamer, SimplePanel simplePanel, HTML detailsView) {
    this.entityNamer = entityNamer;
    this.panel = simplePanel;
    this.detailsView = detailsView;
  }

  public void onPlaceChanged(PlaceChanged event) {
    if (!(event.getNewPlace() instanceof ApplicationDetailsPlace)) {
      return;
    }
    ApplicationDetailsPlace newPlace = (ApplicationDetailsPlace) event.getNewPlace();
    final Record entity = newPlace.getEntity();

    final String title = new StringBuilder("<h1>").append(entityNamer.render(entity)).append("</h1>").toString();

    final StringBuilder list = new StringBuilder();

    ScaffoldDetailsViewBuilder.appendHtmlDescription(list, entity);
    
    if (list.length() == 0) {
    	// We need to do one by hand
    	System.err.println("Unable to locate a scaffolded details view for " + newPlace);
    }
    
    detailsView.setHTML(title + list.toString());

    if (detailsView.getParent() == null) {
      panel.clear();
      panel.add(detailsView);
    }
  }
}
