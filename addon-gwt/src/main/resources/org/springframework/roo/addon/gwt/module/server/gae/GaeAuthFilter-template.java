package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A servlet filter that handles basic GAE user authentication.
 */
public class GaeAuthFilter implements Filter {

	public void destroy() {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
	                     FilterChain filterChain) throws IOException, ServletException {
		UserService userService = UserServiceFactory.getUserService();
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		if (!userService.isUserLoggedIn()) {
			String requestUrl = request.getHeader("requestUrl");
			if (requestUrl == null) {
				requestUrl = request.getRequestURI();
			}
			response.setHeader("login", userService.createLoginURL(requestUrl));
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		filterChain.doFilter(request, response);
	}

	public void init(FilterConfig config) {
	}
}
