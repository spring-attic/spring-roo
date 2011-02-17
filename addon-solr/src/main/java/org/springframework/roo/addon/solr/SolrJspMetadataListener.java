package org.springframework.roo.addon.solr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperationsImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
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
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Metadata listener responsible for installing Web MVC JSP artifacts for the Solr search addon.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component(immediate = true)
@Service
public final class SolrJspMetadataListener implements MetadataProvider, MetadataNotificationListener {
	@Reference private MetadataDependencyRegistry metadataDependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;
	@Reference private TilesOperations tilesOperations;
	@Reference private MenuOperations menuOperations;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private ProjectOperations projectOperations;
	
	private WebScaffoldMetadata webScaffoldMetadata;
	private EntityMetadata entityMetadata;
	private JavaType javaType;
	private JavaType formbackingObject;
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(SolrWebSearchMetadata.getMetadataIdentiferType(), getProvidesType());
	}

	public MetadataItem get(String metadataIdentificationString) {
		javaType = SolrJspMetadata.getJavaType(metadataIdentificationString);
		Path path = SolrJspMetadata.getPath(metadataIdentificationString);
		String solrWebSearchMetadataKeyString = SolrWebSearchMetadata.createIdentifier(javaType, path);
		SolrWebSearchMetadata webSearchMetadata = (SolrWebSearchMetadata) metadataService.get(solrWebSearchMetadataKeyString);
		if (webSearchMetadata == null || !webSearchMetadata.isValid()) {
			return null;
		}
		
		webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(WebScaffoldMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		Assert.notNull(webScaffoldMetadata, "Web scaffold metadata required");
		
		formbackingObject = webScaffoldMetadata.getAnnotationValues().getFormBackingObject();
		
		entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(formbackingObject, path));
		Assert.notNull(entityMetadata, "Could not determine entity metadata for type: " + javaType.getFullyQualifiedTypeName());
		
		installMvcArtifacts(javaType, path);
		
		return new SolrJspMetadata(metadataIdentificationString, webSearchMetadata);
	}
	
	public void installMvcArtifacts(JavaType javaType, Path path) {
		copyArtifacts("form/search.tagx", "WEB-INF/tags/form/search.tagx");
		copyArtifacts("form/fields/search-facet.tagx", "WEB-INF/tags/form/fields/search-facet.tagx");
		copyArtifacts("form/fields/search-field.tagx", "WEB-INF/tags/form/fields/search-field.tagx");
		
		writeToDiskIfNecessary(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views/" + webScaffoldMetadata.getAnnotationValues().getPath() + "/search.jspx"), getSearchDocument());
		
		String folderName = webScaffoldMetadata.getAnnotationValues().getPath();
		tilesOperations.addViewDefinition(folderName, folderName + "/search", TilesOperationsImpl.DEFAULT_TEMPLATE, "/WEB-INF/views/" + webScaffoldMetadata.getAnnotationValues().getPath() + "/search.jspx");
		menuOperations.addMenuItem(new JavaSymbolName(formbackingObject.getSimpleTypeName()), new JavaSymbolName("solr"), new JavaSymbolName(entityMetadata.getPlural()).getReadableSymbolName(), "global.menu.find", "/" + webScaffoldMetadata.getAnnotationValues().getPath() + "?search", "s:");
	}
	
	private Document getSearchDocument() {
		// Next install search.jspx
		WebScaffoldMetadata webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(WebScaffoldMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		Assert.notNull(webScaffoldMetadata, "Web scaffold metadata required");

		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();
				
		// Add document namespaces
		Element div = new XmlElementBuilder("div", document)
								.addAttribute("xmlns:page", "urn:jsptagdir:/WEB-INF/tags/form")
								.addAttribute("xmlns:fields", "urn:jsptagdir:/WEB-INF/tags/form/fields")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build();
		document.appendChild(div);		
		
		Element pageSearch = new XmlElementBuilder("page:search", document)
									.addAttribute("id", XmlUtils.convertId("ps:" + webScaffoldMetadata.getAnnotationValues().getFormBackingObject().getFullyQualifiedTypeName()))
									.addAttribute("path", webScaffoldMetadata.getAnnotationValues().getPath())
								.build();
		pageSearch.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(pageSearch));
		
		Element resultTable = new XmlElementBuilder("fields:table", document)
									.addAttribute("id", XmlUtils.convertId("rt:" + webScaffoldMetadata.getAnnotationValues().getFormBackingObject().getFullyQualifiedTypeName()))
									.addAttribute("data", "${searchResults}")
									.addAttribute("delete", "false")
									.addAttribute("update", "false")
									.addAttribute("path", webScaffoldMetadata.getAnnotationValues().getPath())
									.addAttribute("typeIdFieldName", formbackingObject.getSimpleTypeName().toLowerCase() + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName().toLowerCase() + SolrUtils.getSolrDynamicFieldPostFix(entityMetadata.getIdentifierField().getFieldType()))
								.build();
		resultTable.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(resultTable));
					
		StringBuilder facetFields = new StringBuilder();
		int fieldCounter = 0;
		
		PhysicalTypeMetadata formBackingObjectPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(formbackingObject, Path.SRC_MAIN_JAVA));
		Assert.notNull(formBackingObjectPhysicalTypeMetadata, "Unable to obtain physical type metdata for type " + formbackingObject.getFullyQualifiedTypeName());
		ClassOrInterfaceTypeDetails formbackingClassOrInterfaceDetails = (ClassOrInterfaceTypeDetails) formBackingObjectPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), formbackingClassOrInterfaceDetails);
		
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			// Only interested in accessors
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if(++fieldCounter < 7) {
				if (method.getMethodName().equals(entityMetadata.getIdentifierAccessor().getMethodName()) ||
						method.getMethodName().equals(entityMetadata.getVersionAccessor().getMethodName())) {
					continue;
				}
				FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
				Assert.notNull(field, "Could not determine field for accessor: " + method.getMethodName());
				
				facetFields.append(formbackingObject.getSimpleTypeName().toLowerCase()).append(".").append(field.getFieldName()).append(SolrUtils.getSolrDynamicFieldPostFix(field.getFieldType())).append(",");
				
				Element columnElement = new XmlElementBuilder("fields:column", document)
											.addAttribute("id", XmlUtils.convertId("c:" + formbackingObject.getFullyQualifiedTypeName() + "." + field.getFieldName().getSymbolName()))
											.addAttribute("property", formbackingObject.getSimpleTypeName().toLowerCase() + "." + field.getFieldName().getSymbolName().toLowerCase() + SolrUtils.getSolrDynamicFieldPostFix(field.getFieldType()))
										.build();
				columnElement.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(columnElement));
				resultTable.appendChild(columnElement);
			}
		}
		
		Element searchFacet = new XmlElementBuilder("fields:search-facet", document)
									.addAttribute("id", XmlUtils.convertId("sfacet:" + webScaffoldMetadata.getAnnotationValues().getFormBackingObject().getFullyQualifiedTypeName()))
									.addAttribute("facetFields", facetFields.toString())
								.build();
		searchFacet.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(searchFacet));
		pageSearch.appendChild(searchFacet);
		
		Element searchField = new XmlElementBuilder("fields:search-field", document)
				.addAttribute("id", XmlUtils.convertId("sfield:" + webScaffoldMetadata.getAnnotationValues().getFormBackingObject().getFullyQualifiedTypeName()))
			.build();
		searchField.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(searchField));
		
		pageSearch.appendChild(searchFacet);
		pageSearch.appendChild(searchField);
		pageSearch.appendChild(resultTable);
		
		div.appendChild(pageSearch);
		
		return document;
	}
	
	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(String jspFilename, Document proposed) {
		Document original = null;
		
		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(jspFilename)) {	
			try {
				original = XmlUtils.getDocumentBuilder().parse(fileManager.getInputStream(jspFilename));
			} catch (Exception e) {
				new IllegalStateException("Could not parse file: " + jspFilename);
			} 
			Assert.notNull(original, "Unable to parse " + jspFilename);
			if (XmlRoundTripUtils.compareDocuments(original, proposed)) {
				mutableFile = fileManager.updateFile(jspFilename);
			}
		} else {
			original = proposed;
			mutableFile = fileManager.createFile(jspFilename);
			Assert.notNull(mutableFile, "Could not create JSP file '" + jspFilename + "'");
		}
		
		if (mutableFile != null) {
			try {
				// Build a string representation of the JSP
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, original);
				String jspContent = byteArrayOutputStream.toString();

				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(jspContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				// Return and indicate we wrote out the file
				return true;
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
			}
		}
		
		// A file existed, but it contained the same content, so we return false
		return false;
	}

	public String getProvidesType() {
		return SolrJspMetadata.getMetadataIdentiferType();
	}

	public void notify(String upstreamDependency, String downstreamDependency) {
		if (MetadataIdentificationUtils.isIdentifyingClass(downstreamDependency)) {
			Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(SolrWebSearchMetadata.getMetadataIdentiferType())), "Expected class-level notifications only for Solr web search metadata (not '" + upstreamDependency + "')");
			
			// A physical Java type has changed, and determine what the corresponding local metadata identification string would have been
			JavaType javaType = SolrWebSearchMetadata.getJavaType(upstreamDependency);
			Path path = SolrWebSearchMetadata.getPath(upstreamDependency);
			downstreamDependency = SolrJspMetadata.createIdentifier(javaType, path);
			
			// We only need to proceed if the downstream dependency relationship is not already registered
			// (if it's already registered, the event will be delivered directly later on)
			if (metadataDependencyRegistry.getDownstream(upstreamDependency).contains(downstreamDependency)) {
				return;
			}
		}

		// We should now have an instance-specific "downstream dependency" that can be processed by this class
		Assert.isTrue(MetadataIdentificationUtils.getMetadataClass(downstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(getProvidesType())), "Unexpected downstream notification for '" + downstreamDependency + "' to this provider (which uses '" + getProvidesType() + "'");
		
		metadataService.evict(downstreamDependency);
		if (get(downstreamDependency) != null) {
			metadataDependencyRegistry.notifyDownstream(downstreamDependency);
		}
	}
	
	private void copyArtifacts(String relativeTemplateLocation, String relativeProjectFileLocation) {
		// First install search.tagx
		String projectFileLocation = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, relativeProjectFileLocation);
		if (!fileManager.exists(projectFileLocation)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), relativeTemplateLocation), fileManager.createFile(projectFileLocation).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Could not copy " + relativeProjectFileLocation + " into project", e);
			}
		}
	}
}
