package __TOP_LEVEL_PACKAGE__.gwt.place;

import com.google.gwt.valuestore.shared.Record;
import com.springsource.extrack.gwt.place.ApplicationPlace;

/**
 * A place in the app focused on the {@link Record} of a particular type of
 * {@link com.google.gwt.valuestore.shared.ValueStore ValueStore} record.
 */
public abstract class ApplicationValuesPlace extends ApplicationPlace {
  private final Record entity;

  /**
   * @param entity
   */
  public ApplicationValuesPlace(Record entity) {
    this.entity = entity;
  }

  /**
   * @return the entity
   */
  public Record getEntity() {
    return entity;
  }
}
