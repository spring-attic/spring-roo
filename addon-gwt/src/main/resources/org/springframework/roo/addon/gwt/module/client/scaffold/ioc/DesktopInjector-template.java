package __TOP_LEVEL_PACKAGE__.client.scaffold.ioc;

import __TOP_LEVEL_PACKAGE__.client.scaffold.ScaffoldDesktopApp;
import com.google.gwt.inject.client.GinModules;

@GinModules(value = {ScaffoldModule.class})
public interface DesktopInjector extends ScaffoldInjector {

	ScaffoldDesktopApp getScaffoldApp();
}