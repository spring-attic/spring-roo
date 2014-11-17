package org.springframework.roo.addon.finder;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link FinderMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class FinderMetadataProviderImpl extends
        AbstractMemberDiscoveringItdMetadataProvider implements
        FinderMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(FinderMetadataProviderImpl.class);

    private DynamicFinderServices dynamicFinderServices;

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
    	getMetadataDependencyRegistry().addNotificationListener(this);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        // Ignoring trigger annotations means that other MD providers that want
        // to discover whether a type has finders can do so.
        setIgnoreTriggerAnnotations(true);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return FinderMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().removeNotificationListener(this);
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = FinderMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = FinderMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Finder";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        return getLocalMid(itdTypeDetails);
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
    	
    	if(dynamicFinderServices == null){
    		dynamicFinderServices = getDynamicFinderServices();
    	}
    	Validate.notNull(dynamicFinderServices, "DynamicFinderServices is required");
    	
        // We know governor type details are non-null and can be safely cast

        // Work out the MIDs of the other metadata we depend on
        final JavaType javaType = FinderMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = FinderMetadata
                .getPath(metadataIdentificationString);
        final String jpaActiveRecordMetadataKey = JpaActiveRecordMetadata
                .createIdentifier(javaType, path);

        // We need to lookup the metadata we depend on
        final JpaActiveRecordMetadata jpaActiveRecordMetadata = (JpaActiveRecordMetadata) getMetadataService()
                .get(jpaActiveRecordMetadataKey);
        if (jpaActiveRecordMetadata == null
                || !jpaActiveRecordMetadata.isValid()) {
            return new FinderMetadata(metadataIdentificationString, aspectName,
                    governorPhysicalTypeMetadata, null,
                    Collections.<JavaSymbolName, QueryHolder> emptyMap());
        }
        final MethodMetadata entityManagerMethod = jpaActiveRecordMetadata
                .getEntityManagerMethod();
        if (entityManagerMethod == null) {
            return null;
        }

        final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
        if (memberDetails == null) {
            return null;
        }

        final String plural = jpaActiveRecordMetadata.getPlural();
        final String entityName = jpaActiveRecordMetadata.getEntityName();

        // Using SortedMap to ensure that the ITD emits finders in the same
        // order each time
        final SortedMap<JavaSymbolName, QueryHolder> queryHolders = new TreeMap<JavaSymbolName, QueryHolder>();
        for (final String methodName : jpaActiveRecordMetadata
                .getDynamicFinders()) {
            final JavaSymbolName finderName = new JavaSymbolName(methodName);
            final QueryHolder queryHolder = dynamicFinderServices
                    .getQueryHolder(memberDetails, finderName, plural,
                            entityName);
            if (queryHolder != null) {
                queryHolders.put(finderName, queryHolder);
            }
            
            char[] methodNameArray = methodName.toCharArray();
            methodNameArray[0] = Character.toUpperCase(methodNameArray[0]);
            
            final JavaSymbolName countFinderName = new JavaSymbolName("count" + new String(methodNameArray));
            final QueryHolder countQueryHolder = dynamicFinderServices
                    .getCountQueryHolder(memberDetails, finderName, plural,
                            entityName);
            if (countQueryHolder != null) {
                queryHolders.put(countFinderName, countQueryHolder);
            }
        }

        // Now determine all the ITDs we're relying on to ensure we are notified
        // if they change
        for (final QueryHolder queryHolder : queryHolders.values()) {
            for (final Token token : queryHolder.getTokens()) {
                if (token instanceof FieldToken) {
                    final FieldToken fieldToken = (FieldToken) token;
                    final String declaredByMid = fieldToken.getField()
                            .getDeclaredByMetadataId();
                    getMetadataDependencyRegistry().registerDependency(
                            declaredByMid, metadataIdentificationString);
                }
            }
        }

        // We need to be informed if our dependent metadata changes
        getMetadataDependencyRegistry().registerDependency(
                jpaActiveRecordMetadataKey, metadataIdentificationString);

        // We make the queryHolders immutable in case FinderMetadata in the
        // future makes it available through an accessor etc
        return new FinderMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, entityManagerMethod,
                Collections.unmodifiableSortedMap(queryHolders));
    }

    public String getProvidesType() {
        return FinderMetadata.getMetadataIdentiferType();
    }
    
    public DynamicFinderServices getDynamicFinderServices(){
    	// Get all Services implement DynamicFinderServices interface
		try {
			ServiceReference<?>[] references = context.getAllServiceReferences(DynamicFinderServices.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (DynamicFinderServices) context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load DynamicFinderServices on FinderMetadataProviderImpl.");
			return null;
		}
    }
}
