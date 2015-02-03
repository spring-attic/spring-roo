package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.RooJavaType.ROO_SOLR_SEARCHABLE;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadata;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadataProvider;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides {@link SolrMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class SolrMetadataProvider extends
        AbstractMemberDiscoveringItdMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(SolrMetadataProvider.class);
	
    private JpaActiveRecordMetadataProvider jpaActiveRecordMetadataProvider;

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        getJpaActiveRecordMetadataProvider().addMetadataTrigger(ROO_SOLR_SEARCHABLE);
        addMetadataTrigger(ROO_SOLR_SEARCHABLE);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return SolrMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        getJpaActiveRecordMetadataProvider()
                .removeMetadataTrigger(ROO_SOLR_SEARCHABLE);
        removeMetadataTrigger(ROO_SOLR_SEARCHABLE);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = SolrMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = SolrMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "SolrSearch";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        // Determine if this ITD presents a method we're interested in (namely
        // accessors)
        for (final MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
            if (BeanInfoUtils.isAccessorMethod(method)
                    && !method.getMethodName().getSymbolName().startsWith("is")) {
                // We care about this ITD, so formally request an update so we
                // can scan for it and process it

                // Determine the governor for this ITD, and the Path the ITD is
                // stored within
                final JavaType governorType = itdTypeDetails.getName();
                final String providesType = MetadataIdentificationUtils
                        .getMetadataClass(itdTypeDetails
                                .getDeclaredByMetadataId());
                final LogicalPath itdPath = PhysicalTypeIdentifierNamingUtils
                        .getPath(providesType,
                                itdTypeDetails.getDeclaredByMetadataId());

                // Produce the local MID we're going to use and make the request
                return createLocalIdentifier(governorType, itdPath);
            }
        }

        return null;
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // We need to parse the annotation, which we expect to be present
        final SolrSearchAnnotationValues annotationValues = new SolrSearchAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()
                || annotationValues.searchMethod == null) {
            return null;
        }

        // Acquire bean info (we need getters details, specifically)
        final JavaType javaType = SolrMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = SolrMetadata
                .getPath(metadataIdentificationString);
        final String jpaActiveRecordMetadataKey = JpaActiveRecordMetadata
                .createIdentifier(javaType, path);

        // We want to be notified if the getter info changes in any way
        getMetadataDependencyRegistry().registerDependency(
                jpaActiveRecordMetadataKey, metadataIdentificationString);
        final JpaActiveRecordMetadata jpaActiveRecordMetadata = (JpaActiveRecordMetadata) metadataService
                .get(jpaActiveRecordMetadataKey);

        // Abort if we don't have getter information available
        if (jpaActiveRecordMetadata == null
                || !jpaActiveRecordMetadata.isValid()) {
            return null;
        }

        // Otherwise go off and create the Solr metadata
        String beanPlural = javaType.getSimpleTypeName() + "s";
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(PluralMetadata.createIdentifier(javaType, path));
        if (pluralMetadata != null && pluralMetadata.isValid()) {
            beanPlural = pluralMetadata.getPlural();
        }

        final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
        final Map<MethodMetadata, FieldMetadata> accessorDetails = new LinkedHashMap<MethodMetadata, FieldMetadata>();
        for (final MethodMetadata method : memberDetails.getMethods()) {
            if (BeanInfoUtils.isAccessorMethod(method)
                    && !method.getMethodName().getSymbolName().startsWith("is")) {
                final FieldMetadata field = BeanInfoUtils
                        .getFieldForJavaBeanMethod(memberDetails, method);
                if (field != null) {
                    accessorDetails.put(method, field);
                }
                // Track any changes to that method (eg it goes away)
                getMetadataDependencyRegistry().registerDependency(
                        method.getDeclaredByMetadataId(),
                        metadataIdentificationString);
            }
        }
        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(javaType);
        if (identifierAccessor == null) {
            return null;
        }

        final FieldMetadata versionField = persistenceMemberLocator
                .getVersionField(javaType);

        return new SolrMetadata(metadataIdentificationString, aspectName,
                annotationValues, governorPhysicalTypeMetadata,
                identifierAccessor, versionField, accessorDetails, beanPlural);
    }

    public String getProvidesType() {
        return SolrMetadata.getMetadataIdentiferType();
    }
    
    public JpaActiveRecordMetadataProvider getJpaActiveRecordMetadataProvider(){
    	if(jpaActiveRecordMetadataProvider == null){
    		// Get all Services implement JpaActiveRecordMetadataProvider interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(JpaActiveRecordMetadataProvider.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (JpaActiveRecordMetadataProvider) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load JpaActiveRecordMetadataProvider on SolrMetadataProvider.");
    			return null;
    		}
    	}else{
    		return jpaActiveRecordMetadataProvider;
    	}
    }
}