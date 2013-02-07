package org.springframework.roo.addon.layers.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

@Component
@Service
public class ServiceLayerTemplateServiceImpl implements
        ServiceLayerTemplateService {
    @Reference ProjectOperations projectOperations;
    @Reference FileManager fileManager;

    @Override
    public void addServiceToXmlConfiguration(
            ClassOrInterfaceTypeDetails serviceInterface,
            ClassOrInterfaceTypeDetails serviceClass) {
        final PathResolver pathResolver = projectOperations.getPathResolver();

        final String fileIdentifier = pathResolver.getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, "applicationContext-services.xml");

        if (!fileManager.exists(fileIdentifier)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(),
                        "applicationContext-services-template.xml");
                outputStream = fileManager.createFile(fileIdentifier)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        try {
            final DocumentBuilder builder = XmlUtils.getDocumentBuilder();

            InputSource source = new InputSource();
            FileReader fileReader = new FileReader(fileIdentifier);
            source.setCharacterStream(fileReader);
            final Document document = builder.parse(source);

            final String serviceName = StringUtils
                    .uncapitalize(serviceInterface.getType()
                            .getSimpleTypeName());

            Element serviceElement = XmlUtils.findFirstElement("//*[@id='"
                    + serviceName + "']", document.getDocumentElement());

            if (serviceElement != null)
                return;

            serviceElement = document.createElement("bean");
            serviceElement.setAttribute("id", serviceName);
            serviceElement.setAttribute("class", serviceClass.getType()
                    .getFullyQualifiedTypeName());
            Node beansNode = document.getElementsByTagName("beans").item(0);
            if (beansNode.getNodeType() == Node.ELEMENT_NODE) {
                Element beansElement = (Element) beansNode;
                beansElement.appendChild(serviceElement);
                // final Transformer transformer =
                // XmlUtils.createIndentingTransformer();
                TransformerFactory transfac = TransformerFactory.newInstance();
                Transformer transformer = transfac.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                final DOMSource domSource = new DOMSource(document);
                final StreamResult result = new StreamResult(new StringWriter());
                transformer.transform(domSource, result);
                String output = result.getWriter().toString();

                fileManager.createOrUpdateTextFileIfRequired(fileIdentifier,
                        output, true);
            }

        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void removeServiceFromXmlConfiguration(
            ClassOrInterfaceTypeDetails serviceInterface) {
        final PathResolver pathResolver = projectOperations.getPathResolver();

        final String fileIdentifier = pathResolver.getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, "applicationContext-services.xml");

        if (!fileManager.exists(fileIdentifier)) {
            return;
        }

        try {
            final DocumentBuilder builder = XmlUtils.getDocumentBuilder();

            InputSource source = new InputSource();
            FileReader fileReader = new FileReader(fileIdentifier);
            source.setCharacterStream(fileReader);
            final Document document = builder.parse(source);

            final String serviceName = StringUtils
                    .uncapitalize(serviceInterface.getType()
                            .getSimpleTypeName());

            Element serviceElement = XmlUtils.findFirstElement("//*[@id='"
                    + serviceName + "']", document.getDocumentElement());

            if (serviceElement == null)
                return;

            Node beansNode = document.getElementsByTagName("beans").item(0);
            if (beansNode.getNodeType() == Node.ELEMENT_NODE) {
                Element beansElement = (Element) beansNode;
                beansElement.removeChild(serviceElement);
                // final Transformer transformer =
                // XmlUtils.createIndentingTransformer();
                TransformerFactory transfac = TransformerFactory.newInstance();
                Transformer transformer = transfac.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                final DOMSource domSource = new DOMSource(document);
                final StreamResult result = new StreamResult(new StringWriter());
                transformer.transform(domSource, result);
                String output = result.getWriter().toString();

                fileManager.createOrUpdateTextFileIfRequired(fileIdentifier,
                        output, true);
            }

        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }

    }
}
