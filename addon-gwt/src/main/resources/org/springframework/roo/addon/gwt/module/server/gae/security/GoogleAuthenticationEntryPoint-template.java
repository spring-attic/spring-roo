package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class GoogleAuthenticationEntryPoint implements AuthenticationEntryPoint
{
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
			throws IOException, ServletException
	{
		response.sendRedirect("/security/login.jsp");

	}

}
