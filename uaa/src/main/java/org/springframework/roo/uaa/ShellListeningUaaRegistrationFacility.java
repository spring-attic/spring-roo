package org.springframework.roo.uaa;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.ParseResult;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.shell.event.ShellStatus;
import org.springframework.roo.shell.event.ShellStatusListener;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.uaa.client.UaaService;

/**
 * A {@link ShellStatusListener} which determines the bundle symbolic name an executed shell command
 * was provided by and registers the use of that feature in UAA.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
@Component(immediate=true)
public class ShellListeningUaaRegistrationFacility implements ShellStatusListener {
	@Reference private Shell shell;
	@Reference private UaaRegistrationService uaaRegistrationService;
	@Reference UaaService uaaService;
	private BundleContext bundleContext;
	
	protected void activate(ComponentContext context) {
		this.bundleContext = context.getBundleContext();
		shell.addShellStatusListener(this);
	}
	
	protected void deactivate(ComponentContext context) {
		shell.removeShellStatusListener(this);
	}

	public void onShellStatusChange(ShellStatus oldStatus, ShellStatus newStatus) {
		// Handle registering use of a BSN
		ParseResult parseResult = newStatus.getParseResult();
		if (parseResult == null) {
			return;
		}
		// We use the target instance as opposed to the declaring method as we don't want
		// the fact an add-on type inherited from another type to prevent using of that add-on
		// from being detected
		String typeName = parseResult.getInstance().getClass().getName();
		String bundleSymbolicName = BundleFindingUtils.findFirstBundleForTypeName(bundleContext, typeName);
		if (bundleSymbolicName == null) {
			return;
		}

		// UaaRegistrationService deals with determining if the BSN is public (non-public BSNs are not registered)
		uaaRegistrationService.registerBundleSymbolicNameUse(bundleSymbolicName, null);
	}
}
