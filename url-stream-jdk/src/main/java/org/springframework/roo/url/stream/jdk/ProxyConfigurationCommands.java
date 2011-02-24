package org.springframework.roo.url.stream.jdk;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.uaa.client.ProxyService;

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

	@Reference private ProxyService proxyService;

	private static final String NEW_LINE = System.getProperty("line.separator");

	@CliCommand(value="proxy configuration", help="Shows the proxy server configuration")
	public String proxyConfiguration() throws MalformedURLException {
		Proxy p = proxyService.setupProxy(new URL("http://www.springsource.org/roo"));
		StringBuilder sb = new StringBuilder();
		if (p == null) {
			sb.append("                     *** Your system has no proxy setup ***").append(NEW_LINE);
			sb.append("http://download.oracle.com/javase/6/docs/technotes/guides/net/proxies.html offers useful information.").append(NEW_LINE);
			sb.append("For most people, simply edit /etc/java-6-openjdk/net.properties (or equivalent) and set the").append(NEW_LINE);
			sb.append("java.net.useSystemProxies=true property to use your operating system-defined proxy settings.");
		} else {
			sb.append("Proxy to use: ").append(p);
		}
		return sb.toString();
	}
	
}
