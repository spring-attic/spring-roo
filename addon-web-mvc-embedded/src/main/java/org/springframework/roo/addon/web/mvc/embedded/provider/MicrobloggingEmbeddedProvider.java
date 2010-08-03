package org.springframework.roo.addon.web.mvc.embedded.provider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.embedded.AbstractEmbeddedProvider;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Provider to embed micro blog messages via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate=true)
@Service
public class MicrobloggingEmbeddedProvider extends AbstractEmbeddedProvider {
	
	public boolean embed(String url, String viewName) {
		// expected format http://twitter.com/#search?q=@SpringRoo
		if (url.contains("twitter.com")) {
			Map<String, String> options = new HashMap<String, String>();
			options.put("provider", "TWITTER");
			options.put("searchTerm", url.substring(url.indexOf("q=") + 2));
			return install(viewName, options);
		}
		return false;
	}
	
	public boolean install(String viewName, Map<String, String> options) {
		if (options == null || options.size() != 2 || !options.containsKey("provider") || !options.get("provider").equalsIgnoreCase("TWITTER") || !options.containsKey("searchTerm")) { 
			return false;
		}
		String searchTerm = options.get("searchTerm");	
		try {
			searchTerm = URLDecoder.decode(searchTerm, "UTF-8");
		} catch (UnsupportedEncodingException ignore) {}
		installTagx("microblogging");
		Element twitter = new XmlElementBuilder("embed:microblogging", XmlUtils.getDocumentBuilder().newDocument()).addAttribute("id", "twitter_" + searchTerm).addAttribute("searchTerm", searchTerm).build();
		twitter.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(twitter));
		installJspx(getViewName(viewName, options.get("provider").toLowerCase()), null, twitter);
		return true;
	}
}
