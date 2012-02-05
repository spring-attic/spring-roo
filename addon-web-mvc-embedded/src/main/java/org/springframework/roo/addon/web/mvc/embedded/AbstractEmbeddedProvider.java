package org.springframework.roo.addon.web.mvc.embedded;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.addon.web.mvc.jsp.JspOperations;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.url.stream.UrlInputStreamService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Convenience class for the implementation of a {@link EmbeddedProvider}.
 * Offers methods for installing tagx, jspx files and making HTTP requests.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(componentAbstract = true)
public abstract class AbstractEmbeddedProvider implements EmbeddedProvider {

    private static final Logger LOGGER = Logger
            .getLogger(AbstractEmbeddedProvider.class.getName());

    @Reference private FileManager fileManager;
    @Reference private UrlInputStreamService urlInputStreamService;
    @Reference private JspOperations jspOperations;
    @Reference private PathResolver pathResolver;

    /**
     * Convenience method to create a readable title for a EmbeddedCompletor
     * enum value
     * 
     * @param providerName the original String
     * @return the readable String
     */
    private String getTitle(final String providerName) {
        final String[] names = providerName.split("_");
        final StringBuilder sb = new StringBuilder();
        for (final String name : names) {
            sb.append(StringUtils.capitalize(name.toLowerCase()));
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * Convenience method to clean view name of a jspx.
     * 
     * @param viewName the view name to clean
     * @param defaultName the default page name
     * @return the cleaned name
     */
    public String getViewName(String viewName, final String defaultName) {
        if (viewName == null || viewName.length() == 0) {
            viewName = defaultName;
        }
        if (viewName.endsWith(".jspx")) {
            viewName = viewName.substring(0, viewName.indexOf(".") - 1);
        }
        return viewName.toLowerCase();
    }

    /**
     * Method to install jspx file into /WEB-INF/views/embed/ of target project.
     * 
     * @param viewName the jspx file name to install (required)
     * @param title the title of the page to be displayed (not required,
     *            viewName is used alternatively)
     * @param contentElement the DOM element to include into the page.
     */
    public void installJspx(String viewName, String title,
            final Element contentElement) {
        Validate.notBlank(viewName, "View name required");
        Validate.notNull(contentElement, "Content element required");
        if (StringUtils.isBlank(title)) {
            title = getTitle(viewName);
        }
        viewName = getViewName(viewName, "default");
        final String jspx = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views/embed/" + viewName
                        + ".jspx");
        final Document document = contentElement.getOwnerDocument();
        if (!fileManager.exists(jspx)) {
            // Add document namespaces
            final Element div = new XmlElementBuilder("div", document)
                    .addAttribute("xmlns:util",
                            "urn:jsptagdir:/WEB-INF/tags/util")
                    .addAttribute("xmlns:embed",
                            "urn:jsptagdir:/WEB-INF/tags/embed")
                    .addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
                    .addAttribute("version", "2.0")
                    .addChild(
                            new XmlElementBuilder("jsp:output", document)
                                    .addAttribute("omit-xml-declaration", "yes")
                                    .build()).build();
            document.appendChild(div);

            div.appendChild(new XmlElementBuilder("util:panel", document)
                    .addAttribute("id", "title").addAttribute("title", title)
                    .addChild(contentElement).build());

            jspOperations
                    .installView("/embed", viewName, title, "Embedded",
                            document,
                            pathResolver.getFocusedPath(Path.SRC_MAIN_WEBAPP));

        }
        else {
            LOGGER.warning("Could not install jspx with name "
                    + viewName
                    + " because it exists already. Use the --viewName attribute to specify unique name.");
        }
    }

    /**
     * Method to install tagx file into /WEB-INF/tags/embed/ of target project.
     * 
     * @param tagName
     */
    public void installTagx(String tagName) {
        Validate.notBlank(tagName, "Tag name required");

        if (!tagName.endsWith(".tagx")) {
            tagName = tagName.concat(".tagx");
        }
        final String tagx = pathResolver.getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/tags/embed/" + tagName);
        if (!fileManager.exists(tagx)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(), "tags/"
                        + tagName);
                outputStream = fileManager.createFile(tagx).getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not install " + tagx);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    /**
     * Helper method to determine if a given provider is supported by this
     * implementation.
     * 
     * @param candidate the provider name
     * @param completors the completors offered by this implementation
     * @return true if the provider is supported
     */
    public boolean isProviderSupported(final String candidate,
            final EmbeddedCompletor[] completors) {
        for (final EmbeddedCompletor completor : completors) {
            if (completor.name().equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to send a HTTP GET request through the Roo provided
     * infrastructure.
     * 
     * @param urlStr the URL
     * @return the result of the GET request.
     */
    public String sendHttpGetRequest(final String urlStr) {
        Validate.notBlank(urlStr, "URL required");

        String result = null;
        if (urlStr.startsWith("http://")) {
            InputStream inputStream = null;
            try {
                final URL url = new URL(urlStr);
                inputStream = urlInputStreamService.openConnection(url);
                return IOUtils.toString(inputStream);
            }
            catch (final IOException e) {
                LOGGER.warning("Unable to connect to " + urlStr);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return result;
    }
}