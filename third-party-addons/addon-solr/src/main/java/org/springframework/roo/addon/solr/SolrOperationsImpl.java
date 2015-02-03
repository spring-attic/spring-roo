package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.RooJavaType.ROO_SOLR_SEARCHABLE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Search configuration operations.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class SolrOperationsImpl implements SolrOperations {

    @Reference private FileManager fileManager;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public void addAll() {
        final Set<ClassOrInterfaceTypeDetails> cids = typeLocationService
                .findClassesOrInterfaceDetailsWithTag(CustomDataKeys.PERSISTENT_TYPE);
        for (final ClassOrInterfaceTypeDetails cid : cids) {
            if (!Modifier.isAbstract(cid.getModifier())) {
                addSolrSearchableAnnotation(cid);
            }
        }
    }

    public void addSearch(final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");

        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(javaType);
        if (cid == null) {
            throw new IllegalArgumentException("Cannot locate source for '"
                    + javaType.getFullyQualifiedTypeName() + "'");
        }

        if (Modifier.isAbstract(cid.getModifier())) {
            throw new IllegalStateException(
                    "The class specified is an abstract type. Can only add solr search for concrete types.");
        }
        addSolrSearchableAnnotation(cid);
    }

    private void addSolrSearchableAnnotation(
            final ClassOrInterfaceTypeDetails cid) {
        if (cid.getTypeAnnotation(ROO_SOLR_SEARCHABLE) == null) {
            final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    cid);
            cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
                    ROO_SOLR_SEARCHABLE));
            typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
        }
    }

    public boolean isSearchAvailable() {
        return solrPropsInstalled();
    }

    public boolean isSolrInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && !solrPropsInstalled()
                && projectOperations.isFeatureInstalled(FeatureNames.JPA);
    }

    public void setupConfig(final String solrServerUrl) {
        updateConfiguration(projectOperations.getFocusedModuleName());
        updateSolrProperties(solrServerUrl);

        final String contextPath = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
                        "applicationContext.xml");
        final Document appCtx = XmlUtils.readXml(fileManager
                .getInputStream(contextPath));
        final Element root = appCtx.getDocumentElement();

        if (DomUtils.findFirstElementByName("task:annotation-driven", root) == null) {
            if (root.getAttribute("xmlns:task").length() == 0) {
                root.setAttribute("xmlns:task",
                        "http://www.springframework.org/schema/task");
                root.setAttribute(
                        "xsi:schemaLocation",
                        root.getAttribute("xsi:schemaLocation")
                                + "  http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task-3.0.xsd");
            }
            root.appendChild(new XmlElementBuilder("task:annotation-driven",
                    appCtx).addAttribute("executor", "asyncExecutor")
                    .addAttribute("mode", "aspectj").build());
            root.appendChild(new XmlElementBuilder("task:executor", appCtx)
                    .addAttribute("id", "asyncExecutor")
                    .addAttribute("pool-size", "${executor.poolSize}").build());
        }

        final Element solrServer = XmlUtils.findFirstElement(
                "/beans/bean[@id='solrServer']", root);
        if (solrServer != null) {
            return;
        }

        root.appendChild(new XmlElementBuilder("bean", appCtx)
                .addAttribute("id", "solrServer")
                .addAttribute("class",
                        "org.apache.solr.client.solrj.impl.CommonsHttpSolrServer")
                .addChild(
                        new XmlElementBuilder("constructor-arg", appCtx)
                                .addAttribute("value", "${solr.serverUrl}")
                                .build()).build());
        DomUtils.removeTextNodes(root);

        fileManager.createOrUpdateTextFileIfRequired(contextPath,
                XmlUtils.nodeToString(appCtx), false);
    }

    private boolean solrPropsInstalled() {
        return fileManager.exists(projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
                        "solr.properties"));
    }

    private void updateSolrProperties(final String solrServerUrl) {
        final String solrPath = projectOperations.getPathResolver()
                .getFocusedIdentifier(Path.SPRING_CONFIG_ROOT,
                        "solr.properties");
        final boolean solrExists = fileManager.exists(solrPath);

        final Properties props = new Properties();
        InputStream inputStream = null;
        try {
            if (fileManager.exists(solrPath)) {
                inputStream = fileManager.getInputStream(solrPath);
                props.load(inputStream);
            }
        }
        catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }

        props.put("solr.serverUrl", solrServerUrl);
        props.put("executor.poolSize", "10");

        OutputStream outputStream = null;
        try {
            final MutableFile mutableFile = solrExists ? fileManager
                    .updateFile(solrPath) : fileManager.createFile(solrPath);
            outputStream = mutableFile.getOutputStream();
            props.store(outputStream, "Updated at " + new Date());
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    private void updateConfiguration(final String moduleName) {
        final Element configuration = XmlUtils.getConfiguration(getClass());

        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> emailDependencies = XmlUtils.findElements(
                "/configuration/solr/dependencies/dependency", configuration);
        for (final Element dependencyElement : emailDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }
        projectOperations.addDependencies(moduleName, dependencies);
    }
}