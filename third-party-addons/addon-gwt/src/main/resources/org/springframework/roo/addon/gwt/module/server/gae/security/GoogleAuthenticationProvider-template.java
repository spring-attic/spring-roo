package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class GoogleAuthenticationProvider implements AuthenticationProvider
{
	private GoogleUserService googleUserService;

	@Autowired
	public void setGoogleUserService(GoogleUserService googleUserService)
	{
		this.googleUserService = googleUserService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException
	{
		GoogleUser googleUser = googleUserService.findCurrentUser();

		return new GoogleAuthentication(googleUser, authentication.getDetails());
	}

	@Override
	public boolean supports(Class<? extends Object> authentication)
	{
		return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
