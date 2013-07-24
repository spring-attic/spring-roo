package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@Component
public class GoogleAuthenticationFilter extends GenericFilterBean
{
	private AuthenticationManager authenticationManager;
	private AuthenticationDetailsSource<HttpServletRequest, WebAuthenticationDetails> ads = new WebAuthenticationDetailsSource();
	private AuthenticationFailureHandler authenticationFailureHandler = new SimpleUrlAuthenticationFailureHandler();

	private UserService userService = UserServiceFactory.getUserService();

	@Autowired
	public void setAuthenticationManager(AuthenticationManager authenticationManager)
	{
		this.authenticationManager = authenticationManager;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null)
		{
			User user = userService.getCurrentUser();

			if (user != null)
			{
				String email = user.getEmail();

				PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(email, null);
				token.setDetails(ads.buildDetails((HttpServletRequest) request));

				try
				{
					authentication = authenticationManager.authenticate(token);
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
				catch (AuthenticationException ex)
				{
					authenticationFailureHandler.onAuthenticationFailure((HttpServletRequest) request, (HttpServletResponse) response, ex);
				}
			}
		}

		chain.doFilter(request, response);
	}

}
