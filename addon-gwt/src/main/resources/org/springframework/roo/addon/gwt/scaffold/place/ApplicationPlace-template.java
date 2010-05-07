
package __TOP_LEVEL_PACKAGE__.gwt.scaffold.place;

import com.google.gwt.app.place.Place;

/**
 * Base type of {@link Place}s for the Scaffold app.
 */
public abstract class ApplicationPlace extends Place {
  public abstract void accept(ApplicationPlaceProcessor processor);
  
  public abstract <T> T acceptFilter(ApplicationPlaceFilter<T> filter);
}