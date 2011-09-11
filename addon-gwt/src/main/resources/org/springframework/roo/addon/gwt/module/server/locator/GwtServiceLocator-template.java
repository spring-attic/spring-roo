package __TOP_LEVEL_PACKAGE__.server.locator;

import javax.servlet.http.HttpServletRequest;

import com.google.web.bindery.requestfactory.server.RequestFactoryServlet;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class GwtServiceLocator implements ServiceLocator {

	HttpServletRequest request = RequestFactoryServlet.getThreadLocalRequest();
	ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(request.getSession().getServletContext());

	@Override
	public Object getInstance(Class<?> clazz) {
		return context.getBean(clazz);
	}
}
