package org.springframework.roo.url.stream.jdk;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;

/**
 * Provides the ability to configure a proxy server for usage by
 * {@link JdkUrlInputStreamService}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component
@Service
public class ProxyConfigurationCommands implements CommandMarker {

	@CliCommand(value="proxy configuration", help="Shows the proxy server configuration")
	public String proxyConfiguration() {
		return "Proxy configuration unsupported in this version of Spring Roo";
	}
	
	// TODO: Support proxies
	
}
