package __TOP_LEVEL_PACKAGE__.gwt.place;

import com.google.gwt.valuestore.shared.Record;

/**
 * Place in an app that lists
 * {@link com.google.gwt.valuestore.shared.ValueStore ValueStore} records of a
 * particular type.
 */
public class ApplicationListPlace extends ApplicationPlace {
  private final Class<? extends Record> record;

  /**
   * @param key the schema of the entities at this place
   */
  public ApplicationListPlace(Class<? extends Record> record) {
    this.record = record;
  }

  public Class<? extends Record> getRecord() {
    return record;
  }
}
