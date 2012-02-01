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
 * Provider to embed photo galleries via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class PhotoEmbeddedProvider extends AbstractEmbeddedProvider {

    public enum PhotoProvider implements EmbeddedCompletor {
        FLIKR, PICASA;

        @Override
        public String toString() {
            final ToStringBuilder builder = new ToStringBuilder(this);
            builder.append("provider", name());
            return builder.toString();
        }
    }

    public boolean embed(final String url, final String viewName) {
        // Expected http://picasaweb.google.com.au/stsmedia/SydneyByNight
        if (url.contains("picasaweb.google.")) {
            final String[] split = url.split("/");
            if (split.length > 4) {
                final Map<String, String> options = new HashMap<String, String>();
                options.put("provider", PhotoProvider.PICASA.name());
                options.put("userId", split[3]);
                options.put("albumId", getPicasaId(url));
                return install(viewName, options);
            }
            return false;
        }
        else if (url.contains("flickr.")) {
            final String[] split = url.split("/");
            if (split.length > 4) {
                final Map<String, String> options = new HashMap<String, String>();
                options.put("provider", PhotoProvider.FLIKR.name());
                options.put("userId", split[4]);
                options.put("albumId", split.length > 5 ? split[5] : split[4]);
                return install(viewName, options);
            }
            return false;
        }
        return false;
    }

    private String getPicasaId(final String url) {
        final String json = sendHttpGetRequest("http://api.embed.ly/v1/api/oembed?url="
                + url);
        if (json != null) {
            final String subDoc = json
                    .substring(json.indexOf("albumid%2F") + 10);
            return subDoc.substring(
                    0,
                    subDoc.indexOf("%") == 1 ? subDoc.length() : subDoc
                            .indexOf("%"));
        }
        return null;
    }

    public boolean install(final String viewName,
            final Map<String, String> options) {
        if (options == null || options.size() != 3
                || !options.containsKey("provider")
                || !options.containsKey("userId")
                || !options.containsKey("albumId")) {
            return false;
        }
        final String provider = options.get("provider");
        if (!isProviderSupported(provider, PhotoProvider.values())) {
            return false;
        }
        final String userId = options.get("userId");
        final String albumId = options.get("albumId");
        installTagx("photos");
        final Element photos = new XmlElementBuilder("embed:photos", XmlUtils
                .getDocumentBuilder().newDocument())
                .addAttribute("id", "photos_" + userId + "_" + albumId)
                .addAttribute("albumId", albumId)
                .addAttribute("userId", userId)
                .addAttribute("provider", provider.toLowerCase()).build();
        photos.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(photos));
        installJspx(getViewName(viewName, provider.toLowerCase()), null, photos);
        return true;
    }
}
