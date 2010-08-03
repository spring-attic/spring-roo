package org.springframework.roo.addon.web.mvc.embedded.provider;

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
 * Provider to embed a Google wave via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate=true)
@Service
public class WaveEmbeddedProvider extends AbstractEmbeddedProvider {
	
	public boolean embed(String url, String viewName) {
		// expected format https://wave.google.com/wave/#restored:wave:googlewave.com%252Fw%252B8Hj0sgUxC
		if (url.contains("wave.google.")) {			
			String qStart = url.substring(url.indexOf("%252B") + 5);
			Map<String, String> options = new HashMap<String, String>();
			options.put("provider", "GOOGLE_WAVE");
			options.put("id", qStart.substring(0, qStart.indexOf(".") == -1 ? qStart.length() : qStart.indexOf(".")));
			return install(viewName, options);
		}
		return false;
	}
	
	public boolean install(String viewName, Map<String, String> options) {
		if (options == null || options.size() != 2 || !options.containsKey("provider") || !options.get("provider").equalsIgnoreCase("GOOGLE_WAVE") || !options.containsKey("id")) { 
			return false;
		}
		String id = options.get("id");
		installTagx("wave");
		Element wave = new XmlElementBuilder("embed:wave", XmlUtils.getDocumentBuilder().newDocument()).addAttribute("id", "wave_" + id).addAttribute("waveId", id).build();
		wave.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(wave));
		installJspx(getViewName(viewName, options.get("provider").toLowerCase()), null, wave);
		return true;
	}
}
