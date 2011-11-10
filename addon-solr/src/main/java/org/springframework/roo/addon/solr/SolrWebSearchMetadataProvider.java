package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.RooJavaType.ROO_SOLR_WEB_SEARCHABLE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadata;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.Assert;
/**
 * Provides {@link SolrWebSearchMetadata}.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class SolrWebSearchMetadataProvider extends AbstractItdMetadataProvider {

	// Fields
	@Reference private WebScaffoldMetadataProvider webScaffoldMetadataProvider;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		webScaffoldMetadataProvider.addMetadataTrigger(ROO_SOLR_WEB_SEARCHABLE);
		addMetadataTrigger(ROO_SOLR_WEB_SEARCHABLE);
	}

	/**
	 * OSGi bundle deactivation callback
	 *
	 * @param context
	 * @since 1.2.0
	 */
	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		webScaffoldMetadataProvider.removeMetadataTrigger(ROO_SOLR_WEB_SEARCHABLE);
		removeMetadataTrigger(ROO_SOLR_WEB_SEARCHABLE);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		SolrWebSearchAnnotationValues annotationValues = new SolrWebSearchAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getSearchMethod() == null) {
			return null;
		}

		// Acquire bean info (we need getters details, specifically)
		JavaType javaType = SolrWebSearchMetadata.getJavaType(metadataIdentificationString);
		LogicalPath path = SolrWebSearchMetadata.getPath(metadataIdentificationString);
		String webScaffoldMetadataKey = WebScaffoldMetadata.createIdentifier(javaType, path);

		// We want to be notified if the getter info changes in any way
		metadataDependencyRegistry.registerDependency(webScaffoldMetadataKey, metadataIdentificationString);
		WebScaffoldMetadata webScaffoldMetadata = (WebScaffoldMetadata) metadataService.get(webScaffoldMetadataKey);

		// Abort if we don't have getter information available
		if (webScaffoldMetadata == null || !webScaffoldMetadata.isValid()) {
			return null;
		}

		JavaType targetObject = webScaffoldMetadata.getAnnotationValues().getFormBackingObject();
		Assert.notNull(targetObject, "Could not acquire form backing object for the '" + WebScaffoldMetadata.getJavaType(webScaffoldMetadata.getId()).getFullyQualifiedTypeName() + "' controller");

		String targetObjectMid = typeLocationService.getPhysicalTypeIdentifier(targetObject);
		LogicalPath targetObjectPath = PhysicalTypeIdentifier.getPath(targetObjectMid);

		SolrMetadata solrMetadata = (SolrMetadata) metadataService.get(SolrMetadata.createIdentifier(targetObject, targetObjectPath));
		Assert.notNull(solrMetadata, "Could not determine SolrMetadata for type '" + targetObject.getFullyQualifiedTypeName() + "'");

		// Otherwise go off and create the to String metadata
		return new SolrWebSearchMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, webScaffoldMetadata.getAnnotationValues(), solrMetadata.getAnnotationValues());
	}

	public String getItdUniquenessFilenameSuffix() {
		return "SolrWebSearch";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = SolrWebSearchMetadata.getJavaType(metadataIdentificationString);
		LogicalPath path = SolrWebSearchMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
		return SolrWebSearchMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return SolrWebSearchMetadata.getMetadataIdentiferType();
	}
}