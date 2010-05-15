package org.springframework.roo.addon.gwt.scaffold.place;

import com.google.gwt.app.place.Place;

/**
 * Base type of {@link Place}s for the Scaffold app.
 */
public abstract class ScaffoldPlace extends Place {
  public abstract void accept(ScaffoldPlaceProcessor processor);
  
  public abstract <T> T acceptFilter(ScaffoldPlaceFilter<T> filter);
}