package __TOP_LEVEL_PACKAGE__.client.scaffold.ioc;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import __TOP_LEVEL_PACKAGE__.client.scaffold.ScaffoldApp;

@GinModules(value = {ScaffoldModule.class})
public interface ScaffoldInjector extends Ginjector {
	
	ScaffoldApp getScaffoldApp();

}