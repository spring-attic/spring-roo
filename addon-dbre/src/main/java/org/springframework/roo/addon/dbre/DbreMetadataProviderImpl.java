package org.springframework.roo.addon.dbre;

import static org.springframework.roo.model.RooJavaType.ROO_DB_MANAGED;

import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link DbreMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreMetadataProviderImpl extends AbstractItdMetadataProvider
        implements DbreMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(DbreMetadataProviderImpl.class);
	
    private DbreModelService dbreModelService;
    private TypeManagementService typeManagementService;

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_DB_MANAGED);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return DbreMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_DB_MANAGED);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = DbreMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = DbreMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    private IdentifierHolder getIdentifierHolder(final JavaType javaType) {
        final List<FieldMetadata> identifierFields = persistenceMemberLocator
                .getIdentifierFields(javaType);
        if (identifierFields.isEmpty()) {
            return null;
        }

        final FieldMetadata identifierField = identifierFields.get(0);
        final boolean embeddedIdField = identifierField.getCustomData().get(
                CustomDataKeys.EMBEDDED_ID_FIELD) != null;
        final List<FieldMetadata> embeddedIdFields = persistenceMemberLocator
                .getEmbeddedIdentifierFields(javaType);
        return new IdentifierHolder(identifierField, embeddedIdField,
                embeddedIdFields);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "DbManaged";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        // We need to parse the annotation, which we expect to be present
        final DbManagedAnnotationValues annotationValues = new DbManagedAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()) {
            return null;
        }

        // Abort if the database couldn't be deserialized. This can occur if the
        // DBRE XML file has been deleted or is empty.
        final Database database = getDbreModelService().getDatabase(false);
        if (database == null) {
            return null;
        }

        // We know governor type details are non-null and can be safely cast
        final JavaType javaType = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails().getName();
        final IdentifierHolder identifierHolder = getIdentifierHolder(javaType);
        if (identifierHolder == null) {
            return null;
        }

        final FieldMetadata versionField = getVersionField(javaType,
                metadataIdentificationString);

        // Search for database-managed entities
        final Iterable<ClassOrInterfaceTypeDetails> managedEntities = typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_DB_MANAGED);

        boolean found = false;
        for (final ClassOrInterfaceTypeDetails managedEntity : managedEntities) {
            if (managedEntity.getName().equals(javaType)) {
                found = true;
                break;
            }
        }
        if (!found) {
            final String mid = typeLocationService
                    .getPhysicalTypeIdentifier(javaType);
            getMetadataDependencyRegistry().registerDependency(mid,
                    metadataIdentificationString);
            return null;
        }

        final DbreMetadata dbreMetadata = new DbreMetadata(
                metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, annotationValues,
                identifierHolder, versionField, managedEntities, database);
        final ClassOrInterfaceTypeDetails updatedGovernor = dbreMetadata
                .getUpdatedGovernor();
        if (updatedGovernor != null) {
            getTypeManagementService().createOrUpdateTypeOnDisk(updatedGovernor);
        }
        return dbreMetadata;
    }

    public String getProvidesType() {
        return DbreMetadata.getMetadataIdentiferType();
    }

    private FieldMetadata getVersionField(final JavaType domainType,
            final String metadataIdentificationString) {
        return persistenceMemberLocator.getVersionField(domainType);
    }
    
    public DbreModelService getDbreModelService(){
    	if(dbreModelService == null){
    		// Get all Services implement DbreModelService interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(DbreModelService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (DbreModelService) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load DbreModelService on DbreMetadataProviderImpl.");
    			return null;
    		}
    	}else{
    		return dbreModelService;
    	}
    }
    
    public TypeManagementService getTypeManagementService(){
    	if(typeManagementService == null){
    		// Get all Services implement TypeManagementService interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(TypeManagementService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (TypeManagementService) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TypeManagementService on DbreMetadataProviderImpl.");
    			return null;
    		}
    	}else{
    		return typeManagementService;
    	}
    }
}