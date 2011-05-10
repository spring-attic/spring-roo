package org.springframework.roo.addon.web.mvc.embedded.provider;

import java.io.ByteArrayInputStream;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provider to embed videos via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate=true)
@Service
public class VideoEmbeddedProvider extends AbstractEmbeddedProvider {
	
	public boolean embed(String url, String viewName) {
		if (url.contains("youtube.com")) {
			// Expected format: http://www.youtube.com/watch?v=Gb1Z0lfl52I
			Map<String, String> options = new HashMap<String, String>();
			options.put("provider", VideoProvider.YOUTUBE.name());
			options.put("id", url.substring(url.indexOf("v=") + 2));
			return install(viewName, options);
		} else if (url.contains("video.google.")) {
			// Expected format: http://video.google.com/videoplay?docid=1753096859715615067#
			Map<String, String> options = new HashMap<String, String>();
			options.put("provider", VideoProvider.GOOGLE_VIDEO.name());
			options.put("id", url.substring(url.indexOf("docid=") + 6));
			return install(viewName, options);
		} else if (url.contains("vimeo.com")) {
			// Expected format http://vimeo.com/11262623
			Map<String, String> options = new HashMap<String, String>();
			options.put("provider", VideoProvider.VIMEO.name());
			options.put("id", url.substring(url.indexOf("vimeo.com/") + 10));
			return install(viewName, options);
		} else if (url.contains("viddler.com")) {
			// Expected format http://www.viddler.com/explore/failblog/videos/715/
			Map<String, String> options = new HashMap<String, String>();
			options.put("provider", VideoProvider.VIDDLER.name());
			options.put("id", getViddlerId(url));
			return install(viewName, options);
		} else if (url.contains("screenr.com")) {
			// Expected format http://screenr.com/GlR
			String[] split = url.split("/");
			if (split.length > 3) {
				Map<String, String> options = new HashMap<String, String>();
				options.put("provider", VideoProvider.SCREENR.name());
				options.put("id", split[3]);
				return install(viewName, options);
			}
			return false;
		}
		return false;
	}
	
	public boolean install(String viewName, Map<String, String> options) {
		if (options == null || options.size() != 2 || !options.containsKey("provider") || !options.containsKey("id")) { 
			return false;
		}
		String provider = options.get("provider");
		if (!isProviderSupported(provider, VideoProvider.values())) {
			return false;
		}
		String id = options.get("id");
		
		if (VideoProvider.SCREENR.name().equals(provider)) {
			id = getScreenrId("http://screenr.com/" + id);
		}
		if (id == null || id.length() == 0) {
			return false;
		}
		installTagx("video");
		Element video = new XmlElementBuilder("embed:video", XmlUtils.getDocumentBuilder().newDocument()).addAttribute("id", "video_" + id).addAttribute("videoId", id).addAttribute("provider", provider.toLowerCase()).build();
		video.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(video));
		installJspx(getViewName(viewName, provider.toLowerCase()), null, video);
		return true;
	}
	
	private String getScreenrId(String url) {
		String xml = sendHttpGetRequest("http://screenr.com/api/oembed.xml?url=" + url);
		if (xml != null) {
			try {
				Document doc = XmlUtils.getDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
				Element movie = XmlUtils.findRequiredElement("//html", doc.getDocumentElement());
				String movieId = movie.getTextContent();
				movieId = movieId.substring(movieId.indexOf("value=\"i=") + 7);
				return movieId.substring(0, movieId.indexOf("\"") == -1 ? movieId.length() : movieId.indexOf("\""));
			} catch (Exception e) {
				throw new IllegalStateException("Could not parse oembed document from screenr.com", e);
			}
		}
		return null;
	}

	private String getViddlerId(String url) {
		String xml = sendHttpGetRequest("http://lab.viddler.com/services/oembed/?url=" + url + "&type=simple&format=xml");
		if (xml != null) {
			try {
				Document doc = XmlUtils.getDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
				Element movie = XmlUtils.findRequiredElement("//param[@name='movie']", doc.getDocumentElement());
				String movieUrl = movie.getAttribute("value");
				return movieUrl.substring(movieUrl.indexOf("simple/") + 7);
			} catch (Exception e) {
				throw new IllegalStateException("Could not parse oembed document from viddler.com", e);
			}
		}
		return null;
	}
	
	public enum VideoProvider implements EmbeddedCompletor {
		YOUTUBE,
		GOOGLE_VIDEO,
		VIMEO,
		VIDDLER,
		SCREENR;
		
		public String toString() {
			ToStringCreator tsc = new ToStringCreator(this);
			tsc.append("provider", name());
			return tsc.toString();
		}
	}
}
