package org.springframework.roo.addon.solr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.roundtrip.XmlRoundTripFileManager;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Metadata listener responsible for installing Web MVC JSP artifacts for the
 * Solr search addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class SolrJspMetadataListener implements MetadataProvider,
        MetadataNotificationListener {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(SolrJspMetadataListener.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private FileManager fileManager;
    private JavaType formbackingObject;
    private JavaType javaType;
    private JpaActiveRecordMetadata jpaActiveRecordMetadata;
    private MemberDetailsScanner memberDetailsScanner;
    private MenuOperations menuOperations;
    private MetadataDependencyRegistry metadataDependencyRegistry;
    private MetadataService metadataService;
    private PathResolver pathResolver;
    private PersistenceMemberLocator persistenceMemberLocator;

    private TilesOperations tilesOperations;
    private TypeLocationService typeLocationService;
    private WebScaffoldMetadata webScaffoldMetadata;
    private XmlRoundTripFileManager xmlRoundTripFileManager;

    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                SolrWebSearchMetadata.getMetadataIdentiferType(),
                getProvidesType());
    }

    private void copyArtifacts(final String relativeTemplateLocation,
            final String relativeProjectFileLocation) {
        // First install search.tagx
        final String projectFileLocation = getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, relativeProjectFileLocation);
        if (!getFileManager().exists(projectFileLocation)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(),
                        relativeTemplateLocation);
                outputStream = getFileManager().createFile(projectFileLocation)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Could not copy "
                        + relativeProjectFileLocation + " into project", e);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    public MetadataItem get(final String metadataIdentificationString) {
        javaType = SolrJspMetadata.getJavaType(metadataIdentificationString);
        final LogicalPath path = SolrJspMetadata
                .getPath(metadataIdentificationString);
        final String solrWebSearchMetadataKeyString = SolrWebSearchMetadata
                .createIdentifier(javaType, path);
        final SolrWebSearchMetadata webSearchMetadata = (SolrWebSearchMetadata) getMetadataService()
                .get(solrWebSearchMetadataKeyString);
        if (webSearchMetadata == null || !webSearchMetadata.isValid()) {
            return null;
        }

        webScaffoldMetadata = (WebScaffoldMetadata) getMetadataService()
                .get(WebScaffoldMetadata.createIdentifier(javaType, path));
        Validate.notNull(webScaffoldMetadata, "Web scaffold metadata required");

        formbackingObject = webScaffoldMetadata.getAnnotationValues()
                .getFormBackingObject();
        jpaActiveRecordMetadata = (JpaActiveRecordMetadata) getMetadataService()
                .get(JpaActiveRecordMetadata.createIdentifier(
                        formbackingObject, path));
        Validate.notNull(jpaActiveRecordMetadata,
                "Could not determine entity metadata for type: %s",
                javaType.getFullyQualifiedTypeName());

        installMvcArtifacts(webScaffoldMetadata);

        return new SolrJspMetadata(metadataIdentificationString,
                webSearchMetadata);
    }

    public String getProvidesType() {
        return SolrJspMetadata.getMetadataIdentiferType();
    }

    private Document getSearchDocument(
            final WebScaffoldMetadata webScaffoldMetadata) {
        // Next install search.jspx
        Validate.notNull(webScaffoldMetadata, "Web scaffold metadata required");

        final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        final Document document = builder.newDocument();

        // Add document namespaces
        final Element div = new XmlElementBuilder("div", document)
                .addAttribute("xmlns:page", "urn:jsptagdir:/WEB-INF/tags/form")
                .addAttribute("xmlns:fields",
                        "urn:jsptagdir:/WEB-INF/tags/form/fields")
                .addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
                .addAttribute("version", "2.0")
                .addChild(
                        new XmlElementBuilder("jsp:output", document)
                                .addAttribute("omit-xml-declaration", "yes")
                                .build()).build();
        document.appendChild(div);

        final Element pageSearch = new XmlElementBuilder("page:search",
                document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("ps:"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getFormBackingObject()
                                        .getFullyQualifiedTypeName()))
                .addAttribute("path",
                        webScaffoldMetadata.getAnnotationValues().getPath())
                .build();
        pageSearch.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(pageSearch));

        final List<FieldMetadata> idFields = getPersistenceMemberLocator()
                .getIdentifierFields(formbackingObject);
        if (idFields.isEmpty()) {
            return null;
        }
        final Element resultTable = new XmlElementBuilder("fields:table",
                document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("rt:"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getFormBackingObject()
                                        .getFullyQualifiedTypeName()))
                .addAttribute("data", "${searchResults}")
                .addAttribute("delete", "false")
                .addAttribute("update", "false")
                .addAttribute("path",
                        webScaffoldMetadata.getAnnotationValues().getPath())
                .addAttribute(
                        "typeIdFieldName",
                        formbackingObject.getSimpleTypeName().toLowerCase()
                                + "."
                                + idFields.get(0).getFieldName()
                                        .getSymbolName().toLowerCase()
                                + SolrUtils.getSolrDynamicFieldPostFix(idFields
                                        .get(0).getFieldType())).build();
        resultTable.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(resultTable));

        final StringBuilder facetFields = new StringBuilder();
        int fieldCounter = 0;

        final ClassOrInterfaceTypeDetails formbackingClassOrInterfaceDetails = getTypeLocationService()
                .getTypeDetails(formbackingObject);
        Validate.notNull(formbackingClassOrInterfaceDetails,
                "Unable to obtain physical type metadata for type %s",
                formbackingObject.getFullyQualifiedTypeName());
        final MemberDetails memberDetails = getMemberDetailsScanner()
                .getMemberDetails(getClass().getName(),
                        formbackingClassOrInterfaceDetails);
        final MethodMetadata identifierAccessor = getPersistenceMemberLocator()
                .getIdentifierAccessor(formbackingObject);
        final MethodMetadata versionAccessor = getPersistenceMemberLocator()
                .getVersionAccessor(formbackingObject);

        for (final MethodMetadata method : memberDetails.getMethods()) {
            // Only interested in accessors
            if (!BeanInfoUtils.isAccessorMethod(method)) {
                continue;
            }
            if (++fieldCounter < 7) {
                if (method.getMethodName().equals(
                        identifierAccessor.getMethodName())
                        || method.getMethodName().equals(
                                versionAccessor.getMethodName())) {
                    continue;
                }
                if (method.hasSameName(identifierAccessor, versionAccessor)) {
                    continue;
                }

                final FieldMetadata field = BeanInfoUtils
                        .getFieldForJavaBeanMethod(memberDetails, method);
                if (field == null) {
                    continue;
                }

                facetFields
                        .append(formbackingObject.getSimpleTypeName()
                                .toLowerCase())
                        .append(".")
                        .append(field.getFieldName())
                        .append(SolrUtils.getSolrDynamicFieldPostFix(field
                                .getFieldType())).append(",");

                final Element columnElement = new XmlElementBuilder(
                        "fields:column", document)
                        .addAttribute(
                                "id",
                                XmlUtils.convertId("c:"
                                        + formbackingObject
                                                .getFullyQualifiedTypeName()
                                        + "."
                                        + field.getFieldName().getSymbolName()))
                        .addAttribute(
                                "property",
                                formbackingObject.getSimpleTypeName()
                                        .toLowerCase()
                                        + "."
                                        + field.getFieldName().getSymbolName()
                                                .toLowerCase()
                                        + SolrUtils
                                                .getSolrDynamicFieldPostFix(field
                                                        .getFieldType()))
                        .build();
                columnElement.setAttribute("z",
                        XmlRoundTripUtils.calculateUniqueKeyFor(columnElement));
                resultTable.appendChild(columnElement);
            }
        }

        final Element searchFacet = new XmlElementBuilder(
                "fields:search-facet", document)
                .addAttribute(
                        "id",
                        XmlUtils.convertId("sfacet:"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getFormBackingObject()
                                        .getFullyQualifiedTypeName()))
                .addAttribute("facetFields", facetFields.toString()).build();
        searchFacet.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(searchFacet));
        pageSearch.appendChild(searchFacet);

        final Element searchField = new XmlElementBuilder(
                "fields:search-field", document).addAttribute(
                "id",
                XmlUtils.convertId("sfield:"
                        + webScaffoldMetadata.getAnnotationValues()
                                .getFormBackingObject()
                                .getFullyQualifiedTypeName())).build();
        searchField.setAttribute("z",
                XmlRoundTripUtils.calculateUniqueKeyFor(searchField));

        pageSearch.appendChild(searchFacet);
        pageSearch.appendChild(searchField);
        pageSearch.appendChild(resultTable);

        div.appendChild(pageSearch);

        return document;
    }

    public void installMvcArtifacts(
            final WebScaffoldMetadata webScaffoldMetadata) {
        copyArtifacts("form/search.tagx", "WEB-INF/tags/form/search.tagx");
        copyArtifacts("form/fields/search-facet.tagx",
                "WEB-INF/tags/form/fields/search-facet.tagx");
        copyArtifacts("form/fields/search-field.tagx",
                "WEB-INF/tags/form/fields/search-field.tagx");

        final LogicalPath path = WebScaffoldMetadata
                .getPath(webScaffoldMetadata.getId());
        getXmlRoundTripFileManager().writeToDiskIfNecessary(getPathResolver()
                .getIdentifier(
                        Path.SRC_MAIN_WEBAPP.getModulePathId(path.getModule()),
                        "WEB-INF/views/"
                                + webScaffoldMetadata.getAnnotationValues()
                                        .getPath() + "/search.jspx"),
                getSearchDocument(webScaffoldMetadata));

        final String folderName = webScaffoldMetadata.getAnnotationValues()
                .getPath();
        getTilesOperations().addViewDefinition(folderName, path, folderName
                + "/search", TilesOperations.DEFAULT_TEMPLATE, "WEB-INF/views/"
                + webScaffoldMetadata.getAnnotationValues().getPath()
                + "/search.jspx");
        getMenuOperations().addMenuItem(
                new JavaSymbolName(formbackingObject.getSimpleTypeName()),
                new JavaSymbolName("solr"), new JavaSymbolName(
                        jpaActiveRecordMetadata.getPlural())
                        .getReadableSymbolName(), "global.menu.find", "/"
                        + webScaffoldMetadata.getAnnotationValues().getPath()
                        + "?search", "s:", path);
    }

    public void notify(final String upstreamDependency,
            String downstreamDependency) {
        if (MetadataIdentificationUtils
                .isIdentifyingClass(downstreamDependency)) {
            Validate.isTrue(
                    MetadataIdentificationUtils.getMetadataClass(
                            upstreamDependency).equals(
                            MetadataIdentificationUtils
                                    .getMetadataClass(SolrWebSearchMetadata
                                            .getMetadataIdentiferType())),
                    "Expected class-level notifications only for Solr web search metadata (not '%s')",
                    upstreamDependency);

            // A physical Java type has changed, and determine what the
            // corresponding local metadata identification string would have
            // been
            final JavaType javaType = SolrWebSearchMetadata
                    .getJavaType(upstreamDependency);
            final LogicalPath path = SolrWebSearchMetadata
                    .getPath(upstreamDependency);
            downstreamDependency = SolrJspMetadata.createIdentifier(javaType,
                    path);

            // We only need to proceed if the downstream dependency relationship
            // is not already registered
            // (if it's already registered, the event will be delivered directly
            // later on)
            if (getMetadataDependencyRegistry().getDownstream(upstreamDependency)
                    .contains(downstreamDependency)) {
                return;
            }
        }

        // We should now have an instance-specific "downstream dependency" that
        // can be processed by this class
        Validate.isTrue(
                MetadataIdentificationUtils.getMetadataClass(
                        downstreamDependency).equals(
                        MetadataIdentificationUtils
                                .getMetadataClass(getProvidesType())),
                "Unexpected downstream notification for '%s' to this provider (which uses '%s')",
                downstreamDependency, getProvidesType());

        getMetadataService().evict(downstreamDependency);
        if (get(downstreamDependency) != null) {
            getMetadataDependencyRegistry().notifyDownstream(downstreamDependency);
        }
    }
    
    public FileManager getFileManager(){
    	if(fileManager == null){
    		// Get all Services implement FileManager interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (FileManager) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load FileManager on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return fileManager;
    	}
    }
    
    public MemberDetailsScanner getMemberDetailsScanner(){
    	if(memberDetailsScanner == null){
    		// Get all Services implement MemberDetailsScanner interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(MemberDetailsScanner.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (MemberDetailsScanner) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MemberDetailsScanner on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return memberDetailsScanner;
    	}
    }
    
    public MenuOperations getMenuOperations(){
    	if(menuOperations == null){
    		// Get all Services implement MenuOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(MenuOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (MenuOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MenuOperations on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return menuOperations;
    	}
    }
    
    public MetadataDependencyRegistry getMetadataDependencyRegistry(){
    	if(metadataDependencyRegistry == null){
    		// Get all Services implement MetadataDependencyRegistry interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(MetadataDependencyRegistry.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (MetadataDependencyRegistry) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MetadataDependencyRegistry on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return metadataDependencyRegistry;
    	}
    }
    
    public MetadataService getMetadataService(){
    	if(metadataService == null){
    		// Get all Services implement MetadataService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(MetadataService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (MetadataService) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MetadataService on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return metadataService;
    	}
    }
    
    public PathResolver getPathResolver(){
    	if(pathResolver == null){
    		// Get all Services implement PathResolver interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(PathResolver.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (PathResolver) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PathResolver on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return pathResolver;
    	}
    }
    
    public PersistenceMemberLocator getPersistenceMemberLocator(){
    	if(persistenceMemberLocator == null){
    		// Get all Services implement PersistenceMemberLocator interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(PersistenceMemberLocator.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (PersistenceMemberLocator) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PersistenceMemberLocator on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return persistenceMemberLocator;
    	}
    }
    
    public TilesOperations getTilesOperations(){
    	if(tilesOperations == null){
    		// Get all Services implement TilesOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(TilesOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (TilesOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TilesOperations on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return tilesOperations;
    	}
    }
    
    public TypeLocationService getTypeLocationService(){
    	if(typeLocationService == null){
    		// Get all Services implement TypeLocationService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (TypeLocationService) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TypeLocationService on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return typeLocationService;
    	}
    }
    
    public XmlRoundTripFileManager getXmlRoundTripFileManager(){
    	if(xmlRoundTripFileManager == null){
    		// Get all Services implement XmlRoundTripFileManager interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(XmlRoundTripFileManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (XmlRoundTripFileManager) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load XmlRoundTripFileManager on SolrJspMetadataListener.");
    			return null;
    		}
    	}else{
    		return xmlRoundTripFileManager;
    	}
    }
}
