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
 * Provider to embed maps via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate=true)
@Service
public class MapEmbeddedProvider extends AbstractEmbeddedProvider {
	
	public boolean embed(String url, String viewName) {
		 if (url.contains("maps.google")) {
			// expected format http://maps.google.com/maps?q=sydney,+Australia&.. the q= param needs to be present 
			String qStart = url.substring(url.indexOf("q=") + 2);

			Map<String, String> options = new HashMap<String, String>();
			options.put("provider", "GOOGLE_MAPS");
			options.put("location", qStart.substring(0, qStart.indexOf("&") == -1 ? qStart.length() : qStart.indexOf("&")));
			return install(viewName, options);
		}
		return false;
	}
	
	public boolean install(String viewName, Map<String, String> options) {
		if (options == null || options.size() != 2 || !options.containsKey("provider") || !options.get("provider").equalsIgnoreCase("GOOGLE_MAPS") || !options.containsKey("location")) { 
			return false;
		}
		String location = options.get("location");
		try {
			location = URLDecoder.decode(location, "UTF-8");
		} catch (UnsupportedEncodingException ignore) {}
		installTagx("map");
		Element map = new XmlElementBuilder("embed:map", XmlUtils.getDocumentBuilder().newDocument()).addAttribute("id", "map_" + viewName).addAttribute("location", location).build();
		map.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(map));
		installJspx(getViewName(viewName, options.get("provider").toLowerCase()), null, map);
		return true;
	}
}
