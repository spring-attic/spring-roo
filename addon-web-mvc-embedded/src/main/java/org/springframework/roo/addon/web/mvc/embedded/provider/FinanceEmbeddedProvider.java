package org.springframework.roo.addon.web.mvc.embedded.provider;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.embedded.AbstractEmbeddedProvider;

/**
 * Provider to embed finance charts via a URL or specific install method.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class FinanceEmbeddedProvider extends AbstractEmbeddedProvider {

    // TODO : disabled due to ROO-2562
    public boolean embed(final String url, final String viewName) {
        // if (url.contains("wikinvest.com")) {
        // // Expected format http://www.wikinvest.com/wiki/Vmw
        // Map<String, String> options = new HashMap<String, String>();
        // options.put("provider", "FINANCES");
        // options.put("stockSymbol", url.substring(url.indexOf("wiki/") + 5));
        // return install(viewName, options);
        // }
        return false;
    }

    // TODO : disabled due to ROO-2562
    public boolean install(final String viewName,
            final Map<String, String> options) {
        // if (options == null || options.size() != 2 ||
        // !options.containsKey("provider") ||
        // !options.get("provider").equalsIgnoreCase("finances") ||
        // !options.containsKey("stockSymbol")) {
        // return false;
        // }
        // String stockSymbol = options.get("stockSymbol");
        // installTagx("finances");
        // Element finances = new XmlElementBuilder("embed:finances",
        // XmlUtils.getDocumentBuilder().newDocument()).addAttribute("id",
        // "finances_" + stockSymbol).addAttribute("stockSymbol",
        // stockSymbol).build();
        // finances.setAttribute("z",
        // XmlRoundTripUtils.calculateUniqueKeyFor(finances));
        // installJspx(getViewName(viewName,
        // options.get("provider").toLowerCase()), null, finances);
        // return true;
        return false;
    }
}
