package org.springframework.roo.addon.web.mvc.embedded.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.embedded.AbstractEmbeddedProvider;
import org.springframework.roo.addon.web.mvc.embedded.EmbeddedCompletor;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Provider to embed video streams via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class VideoStreamEmbeddedProvider extends AbstractEmbeddedProvider {

    public enum VideoStreamProvider implements EmbeddedCompletor {
        LIVESTREAM, USTREAM;

        @Override
        public String toString() {
            final ToStringBuilder builder = new ToStringBuilder(this);
            builder.append("provider", name());
            return builder.toString();
        }
    }

    public boolean embed(final String url, final String viewName) {
        if (url.contains("ustream.tv")) {
            // Expected format http://www.ustream.tv/flash/live/1/4424524
            final String[] split = url.split("/");
            if (split.length > 6) {
                final Map<String, String> options = new HashMap<String, String>();
                options.put("provider", VideoStreamProvider.USTREAM.name());
                options.put("id", split[6]);
                return install(viewName, options);
            }
            return false;
        }
        else if (url.contains("livestream.com")) {
            // Expected format http://www.livestream.com/wkrg_oil_spill
            final String[] split = url.split("/");
            if (split.length > 3) {
                final Map<String, String> options = new HashMap<String, String>();
                options.put("provider", VideoStreamProvider.LIVESTREAM.name());
                options.put("id", split[3]);
                return install(viewName, options);
            }
            return false;
        }
        return false;
    }

    public boolean install(final String viewName,
            final Map<String, String> options) {
        if (options == null || options.size() != 2
                || !options.containsKey("provider")
                || !options.containsKey("id")) {
            return false;
        }
        final String provider = options.get("provider");
        if (!isProviderSupported(provider, VideoStreamProvider.values())) {
            return false;
        }
        final String id = options.get("id");
        installTagx("videostream");
        final Element video = new XmlElementBuilder("embed:videostream",
                XmlUtils.getDocumentBuilder().newDocument())
                .addAttribute("id", "video_stream_" + id)
                .addAttribute("streamId", id)
                .addAttribute("provider", provider.toLowerCase()).build();
        video.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(video));
        installJspx(getViewName(viewName, provider.toLowerCase()), null, video);
        return true;
    }
}
