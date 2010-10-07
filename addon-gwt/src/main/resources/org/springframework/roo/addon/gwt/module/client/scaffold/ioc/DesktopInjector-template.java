package __TOP_LEVEL_PACKAGE__.client.scaffold.ioc;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import __TOP_LEVEL_PACKAGE__.client.scaffold.ScaffoldApp;
import __TOP_LEVEL_PACKAGE__.client.scaffold.ScaffoldDesktopApp;

@GinModules(value = {ScaffoldModule.class})
public interface DesktopInjector extends ScaffoldInjector {
	
	ScaffoldDesktopApp getScaffoldApp();
}