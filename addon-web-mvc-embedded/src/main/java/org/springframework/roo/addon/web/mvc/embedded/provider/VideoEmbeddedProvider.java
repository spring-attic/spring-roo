package org.springframework.roo.addon.web.mvc.embedded.provider;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.embedded.AbstractEmbeddedProvider;
import org.springframework.roo.addon.web.mvc.embedded.EmbeddedCompletor;
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
@Component
@Service
public class VideoEmbeddedProvider extends AbstractEmbeddedProvider {

    public enum VideoProvider implements EmbeddedCompletor {
        GOOGLE_VIDEO, SCREENR, VIDDLER, VIMEO, YOUTUBE;

        @Override
        public String toString() {
            final ToStringBuilder builder = new ToStringBuilder(this);
            builder.append("provider", name());
            return builder.toString();
        }
    }

    public boolean embed(final String url, final String viewName) {
        if (url.contains("youtube.com")) {
            // Expected format: http://www.youtube.com/watch?v=Gb1Z0lfl52I
            final Map<String, String> options = new HashMap<String, String>();
            options.put("provider", VideoProvider.YOUTUBE.name());
            options.put("id", url.substring(url.indexOf("v=") + 2));
            return install(viewName, options);
        }
        else if (url.contains("video.google.")) {
            // Expected format:
            // http://video.google.com/videoplay?docid=1753096859715615067#
            final Map<String, String> options = new HashMap<String, String>();
            options.put("provider", VideoProvider.GOOGLE_VIDEO.name());
            options.put("id", url.substring(url.indexOf("docid=") + 6));
            return install(viewName, options);
        }
        else if (url.contains("vimeo.com")) {
            // Expected format http://vimeo.com/11262623
            final Map<String, String> options = new HashMap<String, String>();
            options.put("provider", VideoProvider.VIMEO.name());
            options.put("id", url.substring(url.indexOf("vimeo.com/") + 10));
            return install(viewName, options);
        }
        else if (url.contains("viddler.com")) {
            // Expected format
            // http://www.viddler.com/explore/failblog/videos/715/
            final Map<String, String> options = new HashMap<String, String>();
            options.put("provider", VideoProvider.VIDDLER.name());
            options.put("id", getViddlerId(url));
            return install(viewName, options);
        }
        else if (url.contains("screenr.com")) {
            // Expected format http://screenr.com/GlR
            final String[] split = url.split("/");
            if (split.length > 3) {
                final Map<String, String> options = new HashMap<String, String>();
                options.put("provider", VideoProvider.SCREENR.name());
                options.put("id", split[3]);
                return install(viewName, options);
            }
            return false;
        }
        return false;
    }

    private String getViddlerId(final String url) {
        final String xml = sendHttpGetRequest("http://lab.viddler.com/services/oembed/?url="
                + url + "&type=simple&format=xml");
        if (xml != null) {
            try {
                final Document doc = XmlUtils.readXml(new ByteArrayInputStream(
                        xml.getBytes()));
                final Element movie = XmlUtils.findRequiredElement(
                        "//param[@name='movie']", doc.getDocumentElement());
                final String movieUrl = movie.getAttribute("value");
                return movieUrl.substring(movieUrl.indexOf("simple/") + 7);
            }
            catch (final Exception e) {
                throw new IllegalStateException(
                        "Could not parse oembed document from viddler.com", e);
            }
        }
        return null;
    }

    public boolean install(final String viewName,
            final Map<String, String> options) {
        if (options == null || options.size() != 2
                || !options.containsKey("provider")
                || !options.containsKey("id")) {
            return false;
        }
        final String provider = options.get("provider");
        if (!isProviderSupported(provider, VideoProvider.values())) {
            return false;
        }
        final String id = options.get("id");
        if (StringUtils.isBlank(id)) {
            return false;
        }
        installTagx("video");
        final Element video = new XmlElementBuilder("embed:video", XmlUtils
                .getDocumentBuilder().newDocument())
                .addAttribute("id", "video_" + id).addAttribute("videoId", id)
                .addAttribute("provider", provider.toLowerCase()).build();
        video.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(video));
        installJspx(getViewName(viewName, provider.toLowerCase()), null, video);
        return true;
    }
}
