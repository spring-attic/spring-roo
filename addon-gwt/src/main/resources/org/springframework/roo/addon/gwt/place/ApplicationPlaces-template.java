package __TOP_LEVEL_PACKAGE__.gwt.place;

import com.google.gwt.app.place.PlaceController;
import com.google.gwt.bikeshed.cells.client.ActionCell;
import com.google.gwt.valuestore.shared.Record;

/**
 * Object with knowledge of the places of an app.
 */
public class ApplicationPlaces {
  private final PlaceController<ApplicationPlace> controller;

  public ApplicationPlaces(PlaceController<ApplicationPlace> controller) {
    this.controller = controller;
  }

  public <R extends Record> ActionCell.Delegate<R> getDetailsGofer() {
    return new ActionCell.Delegate<R>() {
      public void execute(Record object) {
        goToDetailsFor(object);
      }
    };
  }

  public <R extends Record> ActionCell.Delegate<R> getEditorGofer() {
    return new ActionCell.Delegate<R>() {
      public void execute(Record object) {
        goToEditorFor(object);
      }
    };
  }

  private void goToDetailsFor(Record r) {
    controller.goTo(new ApplicationDetailsPlace(r));
  }

  private void goToEditorFor(Record r) {
    controller.goTo(new ApplicationEditorPlace(r));
  }
}
