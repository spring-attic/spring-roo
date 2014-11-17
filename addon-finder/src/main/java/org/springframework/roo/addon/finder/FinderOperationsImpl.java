package org.springframework.roo.addon.finder;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;

import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link FinderOperations}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class FinderOperationsImpl implements FinderOperations {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(FinderOperationsImpl.class);
    
    // ------------ OSGi component attributes ----------------
   	private BundleContext context;
   	
   	protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    private DynamicFinderServices dynamicFinderServices;
    private MemberDetailsScanner memberDetailsScanner;
    private MetadataService metadataService;
    private PersistenceMemberLocator persistenceMemberLocator;
    private ProjectOperations projectOperations;
    private TypeLocationService typeLocationService;
    private TypeManagementService typeManagementService;

    private String getErrorMsg() {
        return "Annotation " + ROO_JPA_ACTIVE_RECORD.getSimpleTypeName()
                + " attribute 'finders' must be an array of strings";
    }

    public void installFinder(final JavaType typeName,
            final JavaSymbolName finderName) {
        Validate.notNull(typeName, "Java type required");
        Validate.notNull(finderName, "Finer name required");

        final String id = getTypeLocationService()
                .getPhysicalTypeIdentifier(typeName);
        if (id == null) {
            LOGGER.warning("Cannot locate source for '"
                    + typeName.getFullyQualifiedTypeName() + "'");
            return;
        }

        // Go and get the entity metadata, as any type with finders has to be an
        // entity
        final JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
        final LogicalPath path = PhysicalTypeIdentifier.getPath(id);
        final String entityMid = JpaActiveRecordMetadata.createIdentifier(
                javaType, path);

        // Get the entity metadata
        final JpaActiveRecordMetadata jpaActiveRecordMetadata = (JpaActiveRecordMetadata) getMetadataService()
                .get(entityMid);
        if (jpaActiveRecordMetadata == null) {
            LOGGER.warning("Cannot provide finders because '"
                    + typeName.getFullyQualifiedTypeName()
                    + "' is not an entity - " + entityMid);
            return;
        }

        // We know the file exists, as there's already entity metadata for it
        final ClassOrInterfaceTypeDetails cid = getTypeLocationService()
                .getTypeDetails(id);
        if (cid == null) {
            throw new IllegalArgumentException("Cannot locate source for '"
                    + javaType.getFullyQualifiedTypeName() + "'");
        }

        // We know there should be an existing RooEntity annotation
        final List<? extends AnnotationMetadata> annotations = cid
                .getAnnotations();
        final AnnotationMetadata jpaActiveRecordAnnotation = MemberFindingUtils
                .getAnnotationOfType(annotations, ROO_JPA_ACTIVE_RECORD);
        if (jpaActiveRecordAnnotation == null) {
            LOGGER.warning("Unable to find the entity annotation on '"
                    + typeName.getFullyQualifiedTypeName() + "'");
            return;
        }

        // Confirm they typed a valid finder name
        final MemberDetails memberDetails = getMemberDetailsScanner()
                .getMemberDetails(getClass().getName(), cid);
        if (getDynamicFinderServices().getQueryHolder(memberDetails, finderName,
                jpaActiveRecordMetadata.getPlural(),
                jpaActiveRecordMetadata.getEntityName()) == null) {
            LOGGER.warning("Finder name '" + finderName.getSymbolName()
                    + "' either does not exist or contains an error");
            return;
        }

        // Make a destination list to store our final attributes
        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        final List<StringAttributeValue> desiredFinders = new ArrayList<StringAttributeValue>();

        // Copy the existing attributes, excluding the "finder" attribute
        boolean alreadyAdded = false;
        final AnnotationAttributeValue<?> val = jpaActiveRecordAnnotation
                .getAttribute(new JavaSymbolName("finders"));
        if (val != null) {
            // Ensure we have an array of strings
            if (!(val instanceof ArrayAttributeValue<?>)) {
                LOGGER.warning(getErrorMsg());
                return;
            }
            final ArrayAttributeValue<?> arrayVal = (ArrayAttributeValue<?>) val;
            for (final Object o : arrayVal.getValue()) {
                if (!(o instanceof StringAttributeValue)) {
                    LOGGER.warning(getErrorMsg());
                    return;
                }
                final StringAttributeValue sv = (StringAttributeValue) o;
                if (sv.getValue().equals(finderName.getSymbolName())) {
                    alreadyAdded = true;
                }
                desiredFinders.add(sv);
            }
        }

        // Add the desired finder to the end
        if (!alreadyAdded) {
            desiredFinders.add(new StringAttributeValue(new JavaSymbolName(
                    "ignored"), finderName.getSymbolName()));
        }

        // Now let's add the "finders" attribute
        attributes.add(new ArrayAttributeValue<StringAttributeValue>(
                new JavaSymbolName("finders"), desiredFinders));

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                cid);
        final AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(
                ROO_JPA_ACTIVE_RECORD, attributes);
        cidBuilder.updateTypeAnnotation(annotation.build(),
                new HashSet<JavaSymbolName>());
        getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    public boolean isFinderInstallationPossible() {
        return getProjectOperations().isFocusedProjectAvailable()
                && getProjectOperations()
                        .isFeatureInstalledInFocusedModule(FeatureNames.JPA);
    }

    public SortedSet<String> listFindersFor(final JavaType typeName,
            final Integer depth) {
        Validate.notNull(typeName, "Java type required");

        final String id = getTypeLocationService()
                .getPhysicalTypeIdentifier(typeName);
        if (id == null) {
            throw new IllegalArgumentException("Cannot locate source for '"
                    + typeName.getFullyQualifiedTypeName() + "'");
        }

        // Go and get the entity metadata, as any type with finders has to be an
        // entity
        final JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
        final LogicalPath path = PhysicalTypeIdentifier.getPath(id);
        final String entityMid = JpaActiveRecordMetadata.createIdentifier(
                javaType, path);

        // Get the entity metadata
        final JpaActiveRecordMetadata jpaActiveRecordMetadata = (JpaActiveRecordMetadata) getMetadataService()
                .get(entityMid);
        if (jpaActiveRecordMetadata == null) {
            throw new IllegalArgumentException(
                    "Cannot provide finders because '"
                            + typeName.getFullyQualifiedTypeName()
                            + "' is not an 'active record' entity");
        }

        // Get the member details
        final PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) getMetadataService()
                .get(PhysicalTypeIdentifier.createIdentifier(javaType, path));
        if (physicalTypeMetadata == null) {
            throw new IllegalStateException(
                    "Could not determine physical type metadata for type "
                            + javaType);
        }
        final ClassOrInterfaceTypeDetails cid = physicalTypeMetadata
                .getMemberHoldingTypeDetails();
        if (cid == null) {
            throw new IllegalStateException(
                    "Could not determine class or interface type details for type "
                            + javaType);
        }
        final MemberDetails memberDetails = getMemberDetailsScanner()
                .getMemberDetails(getClass().getName(), cid);
        final List<FieldMetadata> idFields = getPersistenceMemberLocator()
                .getIdentifierFields(javaType);
        final FieldMetadata versionField = getPersistenceMemberLocator()
                .getVersionField(javaType);

        // Compute the finders (excluding the ID, version, and EM fields)
        final Set<JavaSymbolName> exclusions = new HashSet<JavaSymbolName>();
        exclusions.add(jpaActiveRecordMetadata.getEntityManagerField()
                .getFieldName());
        for (final FieldMetadata idField : idFields) {
            exclusions.add(idField.getFieldName());
        }

        if (versionField != null) {
            exclusions.add(versionField.getFieldName());
        }

        final SortedSet<String> result = new TreeSet<String>();

        final List<JavaSymbolName> finders = getDynamicFinderServices().getFinders(
                memberDetails, jpaActiveRecordMetadata.getPlural(), depth,
                exclusions);
        for (final JavaSymbolName finder : finders) {
            // Avoid displaying problematic finders
            try {
                final QueryHolder queryHolder = getDynamicFinderServices()
                        .getQueryHolder(memberDetails, finder,
                                jpaActiveRecordMetadata.getPlural(),
                                jpaActiveRecordMetadata.getEntityName());
                final List<JavaSymbolName> parameterNames = queryHolder
                        .getParameterNames();
                final List<JavaType> parameterTypes = queryHolder
                        .getParameterTypes();
                final StringBuilder signature = new StringBuilder();
                int x = -1;
                for (final JavaType param : parameterTypes) {
                    x++;
                    if (x > 0) {
                        signature.append(", ");
                    }
                    signature.append(param.getSimpleTypeName()).append(" ")
                            .append(parameterNames.get(x).getSymbolName());
                }
                result.add(finder.getSymbolName() + "(" + signature + ")" /*
                                                                           * query:
                                                                           * '"
                                                                           * +
                                                                           * query
                                                                           * +
                                                                           * "'"
                                                                           */);
            }
            catch (final RuntimeException e) {
                result.add(finder.getSymbolName() + " - failure");
            }
        }
        return result;
    }
    
    public DynamicFinderServices getDynamicFinderServices(){
    	if(dynamicFinderServices == null){
        	// Get all Services implement DynamicFinderServices interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(DynamicFinderServices.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (DynamicFinderServices) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load DynamicFinderServices on FinderOperationsImpl.");
    			return null;
    		}
    	}else{
    		return dynamicFinderServices;
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
    			LOGGER.warning("Cannot load MemberDetailsScanner on FinderOperationsImpl.");
    			return null;
    		}
    	}else{
    		return memberDetailsScanner;
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
    			LOGGER.warning("Cannot load MetadataService on FinderOperationsImpl.");
    			return null;
    		}
    	}else{
    		return metadataService;
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
    			LOGGER.warning("Cannot load PersistenceMemberLocator on FinderOperationsImpl.");
    			return null;
    		}
    	}else{
    		return persistenceMemberLocator;
    	}
    }
    
    public ProjectOperations getProjectOperations(){
    	if(projectOperations == null){
        	// Get all Services implement ProjectOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (ProjectOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load ProjectOperations on FinderOperationsImpl.");
    			return null;
    		}
    	}else{
    		return projectOperations;
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
    			LOGGER.warning("Cannot load TypeLocationService on FinderOperationsImpl.");
    			return null;
    		}
    	}else{
    		return typeLocationService;
    	}
    }
    
    public TypeManagementService getTypeManagementService(){
    	if(typeManagementService == null){
        	// Get all Services implement TypeManagementService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (TypeManagementService) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TypeManagementService on FinderOperationsImpl.");
    			return null;
    		}
    	}else{
    		return typeManagementService;
    	}
    }
}
