package __TOP_LEVEL_PACKAGE__.gwt.place;

import com.google.gwt.valuestore.shared.Record;

/**
 * Place in an app to see details of a particular
 * {@link com.google.gwt.valuestore.shared.ValueStore ValueStore} record.
 */
public class ApplicationDetailsPlace extends ApplicationValuesPlace {

  public ApplicationDetailsPlace(Record entity) {
    // TODO it is bad to be passing the values rather than an id. Want
    // to force UI code to request the data it needs
    super(entity);
  }
}
