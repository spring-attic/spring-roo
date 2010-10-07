package __TOP_LEVEL_PACKAGE__.client.scaffold.ioc;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import __TOP_LEVEL_PACKAGE__.client.scaffold.ScaffoldDesktopApp;
import __TOP_LEVEL_PACKAGE__.client.scaffold.ScaffoldMobileApp;

@GinModules(value = {ScaffoldModule.class})
public interface MobileInjector extends ScaffoldInjector {

    ScaffoldMobileApp getScaffoldApp();
}
