package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class GoogleAuthentication extends PreAuthenticatedAuthenticationToken
{
	private static final long serialVersionUID = 1L;

	private GoogleUser googleUser;
	private Object aCredentials;
	private boolean isAuthenticated;

	public GoogleAuthentication(GoogleUser googleUser, Object aCredentials)
	{
		super(googleUser, aCredentials);
		this.googleUser = googleUser;
		this.aCredentials = aCredentials;
		this.isAuthenticated = googleUser != null;
	}

	public GoogleAuthentication(GoogleUser googleUser, Object aCredentials, Collection<GrantedAuthority> authorities)
	{
		super(googleUser, aCredentials, authorities);
		this.googleUser = googleUser;
		this.aCredentials = aCredentials;
		this.isAuthenticated = googleUser != null;
	}

	@Override
	public String getName()
	{
		return googleUser == null ? "" : googleUser.getEmail();
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities()
	{
		return googleUser == null ? new ArrayList<GrantedAuthority>() : googleUser.getAuthorities();
	}

	@Override
	public Object getCredentials()
	{
		return aCredentials;
	}

	@Override
	public Object getPrincipal()
	{
		return googleUser == null ? "" : googleUser.getEmail();
	}

	@Override
	public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException
	{
		this.isAuthenticated = isAuthenticated;
	}

	@Override
	public boolean isAuthenticated()
	{
		return isAuthenticated;
	}
}
