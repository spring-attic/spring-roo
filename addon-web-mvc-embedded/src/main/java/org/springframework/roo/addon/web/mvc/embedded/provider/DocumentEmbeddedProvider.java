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
 * Provider to embed documents via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class DocumentEmbeddedProvider extends AbstractEmbeddedProvider {

    public enum DocumentProvider implements EmbeddedCompletor {
        GOOGLE_PRESENTATION, SCRIBD, SLIDESHARE;

        @Override
        public String toString() {
            final ToStringBuilder builder = new ToStringBuilder(this);
            builder.append("provider", name());
            return builder.toString();
        }
    }

    public boolean embed(final String url, final String viewName) {
        if (url.contains("slideshare.net")) {
            // Expected format
            // http://www.slideshare.net/schmidtstefan/spring-one2-gx-slides-stefan-schmidt
            final Map<String, String> options = new HashMap<String, String>();
            options.put("provider", DocumentProvider.SLIDESHARE.name());
            options.put("id", url);
            return install(viewName, options);
        }
        else if (url.contains("scribd.com")) {
            // Expected format
            // http://www.scribd.com/doc/27766735/Introduction-to-SpringRoo
            final String[] split = url.split("/");
            if (split.length > 4) {
                final Map<String, String> options = new HashMap<String, String>();
                options.put("provider", DocumentProvider.SCRIBD.name());
                options.put("id", split[4]);
                return install(viewName, options);
            }
            return false;
        }
        else if (url.contains("docs.google.") && url.contains("present")) {
            // Expected format
            // http://docs.google.com/present/view?id=dd8rf8t9_31c9f2fcgd&revision=_latest&start=0&theme=blank&authkey=CLj5iZwJ&cwj=true
            final String qStart = url.substring(url.indexOf("id=") + 3);
            final Map<String, String> options = new HashMap<String, String>();
            options.put("provider", DocumentProvider.GOOGLE_PRESENTATION.name());
            options.put("id", qStart.substring(
                    0,
                    qStart.indexOf("&") == -1 ? qStart.length() : qStart
                            .indexOf("&")));
            return install(getViewName(viewName, "googlepresentation"), options);
        }
        return false;
    }

    private String getSlideShareId(final String url) {
        final String json = sendHttpGetRequest("http://oohembed.com/oohembed/?url="
                + url.replace(":", "%3A"));
        if (json != null) {
            final String subDoc = json.substring(json.indexOf("doc=") + 4);
            return subDoc.substring(
                    0,
                    subDoc.indexOf("&") == -1 ? subDoc.length() : subDoc
                            .indexOf("&"));
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
        if (!isProviderSupported(provider, DocumentProvider.values())) {
            return false;
        }
        String id = options.get("id");
        installTagx("document");
        if (DocumentProvider.SLIDESHARE.name().equals(provider)
                && id.startsWith("http")) {
            id = getSlideShareId(id);
        }
        final Element document = new XmlElementBuilder("embed:document",
                XmlUtils.getDocumentBuilder().newDocument())
                .addAttribute("id", "doc_" + id).addAttribute("documentId", id)
                .addAttribute("provider", provider.toLowerCase()).build();
        document.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(document));
        installJspx(getViewName(viewName, provider.toLowerCase()), null,
                document);
        return true;
    }
}
