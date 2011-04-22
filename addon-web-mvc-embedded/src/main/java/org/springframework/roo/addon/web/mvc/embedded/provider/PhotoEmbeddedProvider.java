package org.springframework.roo.addon.web.mvc.embedded.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.embedded.AbstractEmbeddedProvider;
import org.springframework.roo.addon.web.mvc.embedded.EmbeddedCompletor;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Provider to embed photo galleries via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate=true)
@Service
public class PhotoEmbeddedProvider extends AbstractEmbeddedProvider {
	
	public boolean embed(String url, String viewName) {
		// Expected http://picasaweb.google.com.au/stsmedia/SydneyByNight
		if (url.contains("picasaweb.google.")) {
			String [] split = url.split("/");
			if (split.length > 4) {
				Map<String, String> options = new HashMap<String, String>();
				options.put("provider", PhotoProvider.PICASA.name());
				options.put("userId", split[3]);
				options.put("albumId", getPicasaId(url));
				return install(viewName, options);
			}
			return false;
		} else if (url.contains("flickr.")) {
			String [] split = url.split("/");
			if (split.length > 4) {
				Map<String, String> options = new HashMap<String, String>();
				options.put("provider", PhotoProvider.FLIKR.name());
				options.put("userId", split[4]);
				options.put("albumId", split.length > 5 ? split[5] : split[4]);
				return install(viewName, options);
			}
			return false;
		}
		return false;
	}
	
	public boolean install(String viewName, Map<String, String> options) {
		if (options == null || options.size() != 3 || !options.containsKey("provider") || !options.containsKey("userId") || !options.containsKey("albumId")) { 
			return false;
		}
		String provider = options.get("provider");
		if (!isProviderSupported(provider, PhotoProvider.values())) {
			return false;
		}
		String userId = options.get("userId");
		String albumId = options.get("albumId");
		installTagx("photos");
		Element photos = new XmlElementBuilder("embed:photos", XmlUtils.getDocumentBuilder().newDocument()).addAttribute("id", "photos_" + userId + "_" + albumId).addAttribute("albumId", albumId).addAttribute("userId", userId).addAttribute("provider", provider.toLowerCase()).build();
		photos.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(photos));
		installJspx(getViewName(viewName, provider.toLowerCase()), null, photos);
		return true;
	}

	private String getPicasaId(String url) {
		String json = sendHttpGetRequest("http://api.embed.ly/v1/api/oembed?url=" + url);
		if (json != null) {
			String subDoc = json.substring(json.indexOf("albumid%2F") + 10);
			return subDoc.substring(0, subDoc.indexOf("%") == 1 ? subDoc.length() : subDoc.indexOf("%"));
		}	
		return null;
	}
	
	public enum PhotoProvider implements EmbeddedCompletor {
		PICASA,
		FLIKR;
		
		public String toString() {
			ToStringCreator tsc = new ToStringCreator(this);
			tsc.append("provider", name());
			return tsc.toString();
		}
	}
}
