package __TOP_LEVEL_PACKAGE__.__SEGMENT_PACKAGE__;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

import com.tvt.fieldmanager.shared.gae.GaeUser;

public interface GoogleUser
{
	Collection<GrantedAuthority> getAuthorities();

	String getNickname();

	String getEmail();

}
