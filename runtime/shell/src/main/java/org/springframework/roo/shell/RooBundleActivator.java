package org.springframework.roo.shell;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleListener;

/**
 * 
 * This class detects if some bundle was installed, activated, modified, etc...
 * and provides information about last time bundle change.
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 *
 */
public interface RooBundleActivator extends BundleActivator, BundleListener {

  /**
   * @return the lastTimeChange
   */
  public Long getLastTimeBundleChange();

  /**
   * @param lastTimeChange the lastTimeChange to set
   */
  public void setLastTimeBundleChange(Long lastTime);



}
