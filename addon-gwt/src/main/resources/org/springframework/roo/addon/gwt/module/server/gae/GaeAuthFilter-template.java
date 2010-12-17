package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
      response.setHeader("login", userService.createLoginURL(request.getRequestURI()));
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return; 
    } 

    filterChain.doFilter(request, response);
  }

  public void init(FilterConfig config) {
  }
}
