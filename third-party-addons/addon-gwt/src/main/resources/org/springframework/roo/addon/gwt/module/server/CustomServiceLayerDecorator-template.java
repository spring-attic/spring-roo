package __TOP_LEVEL_PACKAGE__.server;

import com.google.web.bindery.requestfactory.server.ServiceLayerDecorator;
import com.google.web.bindery.requestfactory.shared.Locator;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class CustomServiceLayerDecorator extends ServiceLayerDecorator {

	@Override
	public <T extends Locator<?, ?>> T createLocator(Class<T> clazz) {
		ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(CustomRequestFactoryServlet.getThreadLocalServletContext());
		return context.getBean(clazz);
	}
}
