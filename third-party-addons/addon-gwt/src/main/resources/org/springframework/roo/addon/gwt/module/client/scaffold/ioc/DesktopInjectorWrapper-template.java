package __TOP_LEVEL_PACKAGE__.client.scaffold.ioc;

import com.google.gwt.core.client.GWT;

public class DesktopInjectorWrapper implements InjectorWrapper {

	public ScaffoldInjector getInjector() {
		return GWT.create(DesktopInjector.class);
	}
}
