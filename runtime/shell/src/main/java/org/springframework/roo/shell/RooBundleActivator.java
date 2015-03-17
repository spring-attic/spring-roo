package org.springframework.roo.shell;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;

/**
 * 
 * This class detects if some bundle was installed, activated, modified, etc...
 * and provides information about if is necessary to refresh command list or not
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 *
 */
@Component(immediate = true)
@Service
public class RooBundleActivator implements BundleActivator, BundleListener {

	private static boolean updateCommands = false;

	protected void activate(ComponentContext context) throws Exception {
		start(context.getBundleContext());
	}

	@Override
	public void start(BundleContext context) throws Exception {
		context.addBundleListener(this);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context.removeBundleListener(this);
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		setUpdateCommands(true);
	}

	/**
	 * @return the updateCommands
	 */
	public static boolean getUpdateCommands() {
		return updateCommands;
	}

	/**
	 * @param updateCommands the updateCommands to set
	 */
	public static void setUpdateCommands(boolean upCommands) {
		updateCommands = upCommands;
	}

}
