package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_MUTATOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.model.RooJavaType.ROO_MONGO_ENTITY;

import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.AnnotatedTypeMatcher;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link MongoEntityMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class MongoEntityMetadataProviderImpl extends
        AbstractItdMetadataProvider implements MongoEntityMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(MongoEntityMetadataProviderImpl.class);
	
    private static final FieldMatcher ID_FIELD_MATCHER = new FieldMatcher(
            IDENTIFIER_FIELD,
            AnnotationMetadataBuilder.getInstance(SpringJavaType.DATA_ID
                    .getFullyQualifiedTypeName()));
    private static final MethodMatcher ID_ACCESSOR_MATCHER = new MethodMatcher(
            Arrays.asList(ID_FIELD_MATCHER), IDENTIFIER_ACCESSOR_METHOD, true);
    private static final MethodMatcher ID_MUTATOR_MATCHER = new MethodMatcher(
            Arrays.asList(ID_FIELD_MATCHER), IDENTIFIER_MUTATOR_METHOD, false);
    private static final AnnotatedTypeMatcher PERSISTENT_TYPE_MATCHER = new AnnotatedTypeMatcher(
            PERSISTENT_TYPE, ROO_MONGO_ENTITY);

    private CustomDataKeyDecorator customDataKeyDecorator;

    @SuppressWarnings("unchecked")
    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        super.setDependsOnGovernorBeingAClass(false);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_MONGO_ENTITY);
        getCustomDataKeyDecorator().registerMatchers(getClass(),
                PERSISTENT_TYPE_MATCHER, ID_FIELD_MATCHER, ID_ACCESSOR_MATCHER,
                ID_MUTATOR_MATCHER);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return MongoEntityMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_MONGO_ENTITY);
        getCustomDataKeyDecorator().unregisterMatchers(getClass());
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = MongoEntityMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = MongoEntityMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Mongo_Entity";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalType,
            final String itdFilename) {
        final MongoEntityAnnotationValues annotationValues = new MongoEntityAnnotationValues(
                governorPhysicalType);
        final JavaType identifierType = annotationValues.getIdentifierType();
        if (!annotationValues.isAnnotationFound() || identifierType == null) {
            return null;
        }

        // Get the governor's member details
        final MemberDetails memberDetails = getMemberDetails(governorPhysicalType);
        if (memberDetails == null) {
            return null;
        }

        final MongoEntityMetadata parent = getParentMetadata(governorPhysicalType
                .getMemberHoldingTypeDetails());

        // If the parent is null, but the type has a super class it is likely
        // that the we don't have information to proceed
        if (parent == null
                && governorPhysicalType.getMemberHoldingTypeDetails()
                        .getSuperclass() != null) {
            // If the superclass is not annotated with the RooMongoEntity
            // trigger
            // annotation then we can be pretty sure that we don't have enough
            // information to proceed
            if (MemberFindingUtils.getAnnotationOfType(governorPhysicalType
                    .getMemberHoldingTypeDetails().getAnnotations(),
                    ROO_MONGO_ENTITY) != null) {
                return null;
            }
        }

        return new MongoEntityMetadata(metadataIdentificationString,
                aspectName, governorPhysicalType, parent, identifierType,
                memberDetails);
    }

    public String getProvidesType() {
        return MongoEntityMetadata.getMetadataIdentiferType();
    }
    
    public CustomDataKeyDecorator getCustomDataKeyDecorator(){
    	if(customDataKeyDecorator == null){
    		// Get all Services implement CustomDataKeyDecorator interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(CustomDataKeyDecorator.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (CustomDataKeyDecorator) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load CustomDataKeyDecorator on MongoEntityMetadataProviderImpl.");
    			return null;
    		}
    	}else{
    		return customDataKeyDecorator;
    	}
    }
}
