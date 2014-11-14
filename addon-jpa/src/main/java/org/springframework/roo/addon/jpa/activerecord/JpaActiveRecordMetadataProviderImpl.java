package org.springframework.roo.addon.jpa.activerecord;

import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.CLEAR_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.COUNT_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_ALL_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FIND_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.FLUSH_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.MERGE_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.PERSIST_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord.REMOVE_METHOD_DEFAULT;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_SORTED_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_SORTED_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jpa.entity.JpaEntityAnnotationValues;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.plural.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link JpaActiveRecordMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JpaActiveRecordMetadataProviderImpl extends
        AbstractItdMetadataProvider implements JpaActiveRecordMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(JpaActiveRecordMetadataProviderImpl.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private ConfigurableMetadataProvider configurableMetadataProvider;
    private CustomDataKeyDecorator customDataKeyDecorator;
    private PluralMetadataProvider pluralMetadataProvider;
    private ProjectOperations projectOperations;

    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
        /*metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());*/
        addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        /*configurableMetadataProvider.addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        pluralMetadataProvider.addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        registerMatchers();*/
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JpaActiveRecordMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        /*metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());*/
        removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        /*configurableMetadataProvider
                .removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        pluralMetadataProvider.removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        customDataKeyDecorator.unregisterMatchers(getClass());*/
    }

    public JpaCrudAnnotationValues getAnnotationValues(final JavaType javaType) {
        Validate.notNull(javaType, "JavaType required");
        final String physicalTypeId = typeLocationService
                .getPhysicalTypeIdentifier(javaType);
        if (StringUtils.isBlank(physicalTypeId)) {
            return null;
        }
        final MemberHoldingTypeDetailsMetadataItem<?> governor = (MemberHoldingTypeDetailsMetadataItem<?>) metadataService
                .get(physicalTypeId);
        if (MemberFindingUtils.getAnnotationOfType(governor,
                ROO_JPA_ACTIVE_RECORD) == null) {
            // The type is not annotated with @RooJpaActiveRecord
            return null;
        }
        return new JpaCrudAnnotationValues(governor);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = JpaActiveRecordMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = JpaActiveRecordMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Jpa_ActiveRecord";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalType,
            final String itdFilename) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        // Get the CRUD-related annotation values
        final JpaCrudAnnotationValues crudAnnotationValues = new JpaCrudAnnotationValues(
                governorPhysicalType);
        // Get the purely JPA-related annotation values, from @RooJpaEntity if
        // present, otherwise from @RooJpaActiveRecord
        JpaEntityAnnotationValues jpaEntityAnnotationValues = new JpaEntityAnnotationValues(
                governorPhysicalType, ROO_JPA_ENTITY);
        if (!jpaEntityAnnotationValues.isAnnotationFound()) {
            jpaEntityAnnotationValues = new JpaEntityAnnotationValues(
                    governorPhysicalType, ROO_JPA_ACTIVE_RECORD);
            Validate.validState(jpaEntityAnnotationValues.isAnnotationFound(),
                    "No @RooJpaEntity or @RooJpaActiveRecord on %s",
                    metadataIdentificationString);
        }

        // Look up the inheritance hierarchy for existing
        // JpaActiveRecordMetadata
        final JpaActiveRecordMetadata parent = getParentMetadata(governorPhysicalType
                .getMemberHoldingTypeDetails());

        // If the parent is null, but the type has a super class it is likely
        // that the we don't have information to proceed
        if (parent == null
                && governorPhysicalType.getMemberHoldingTypeDetails()
                        .getSuperclass() != null) {
            // If the superclass is not annotated with the Entity trigger
            // annotation then we can be pretty sure that we don't have enough
            // information to proceed
            if (MemberFindingUtils.getAnnotationOfType(governorPhysicalType
                    .getMemberHoldingTypeDetails().getAnnotations(),
                    ROO_JPA_ACTIVE_RECORD) != null) {
                return null;
            }
        }
        // We also need the plural
        final JavaType entity = JpaActiveRecordMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = JpaActiveRecordMetadata
                .getPath(metadataIdentificationString);
        final String pluralId = PluralMetadata.createIdentifier(entity, path);
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(pluralId);
        if (pluralMetadata == null) {
            // Can't acquire the plural
            return null;
        }
        metadataDependencyRegistry.registerDependency(pluralId,
                metadataIdentificationString);

        final List<FieldMetadata> idFields = persistenceMemberLocator
                .getIdentifierFields(entity);
        if (idFields.size() != 1) {
            // The ID field metadata is either unavailable or not stable yet
            return null;
        }
        final FieldMetadata idField = idFields.get(0);

        final String entityName = StringUtils.defaultIfEmpty(
                jpaEntityAnnotationValues.getEntityName(),
                entity.getSimpleTypeName());

        boolean isGaeEnabled = false;

        final String moduleName = path.getModule();
        if (projectOperations.isProjectAvailable(moduleName)) {
            // If the project itself changes, we want a chance to refresh this
            // item
            metadataDependencyRegistry.registerDependency(
                    ProjectMetadata.getProjectIdentifier(moduleName),
                    metadataIdentificationString);
            isGaeEnabled = projectOperations.isFeatureInstalledInModule(
                    FeatureNames.GAE, moduleName);
        }

        return new JpaActiveRecordMetadata(metadataIdentificationString,
                aspectName, governorPhysicalType, parent, crudAnnotationValues,
                pluralMetadata.getPlural(), idField, entityName, isGaeEnabled);
    }

    public String getProvidesType() {
        return JpaActiveRecordMetadata.getMetadataIdentifierType();
    }

    @SuppressWarnings("unchecked")
    private void registerMatchers() {
    	
    	if(customDataKeyDecorator == null){
    		customDataKeyDecorator = getCustomDataKeyDecorator();
    	}
    	Validate.notNull(customDataKeyDecorator, "CustomDataKeyDecorator is required");
    	
        customDataKeyDecorator
                .registerMatchers(getClass(),
                        new MethodMatcher(CLEAR_METHOD, ROO_JPA_ACTIVE_RECORD,
                                new JavaSymbolName("clearMethod"),
                                CLEAR_METHOD_DEFAULT), new MethodMatcher(
                                COUNT_ALL_METHOD, ROO_JPA_ACTIVE_RECORD,
                                new JavaSymbolName("countMethod"),
                                COUNT_METHOD_DEFAULT, true, false),
                        new MethodMatcher(FIND_ALL_METHOD,
                                ROO_JPA_ACTIVE_RECORD, new JavaSymbolName(
                                        "findAllMethod"),
                                FIND_ALL_METHOD_DEFAULT, true, false),
                        new MethodMatcher(FIND_ENTRIES_METHOD,
                                ROO_JPA_ACTIVE_RECORD, new JavaSymbolName(
                                        "findEntriesMethod"), "find", false,
                                true, "Entries"), 
                        new MethodMatcher(FIND_ALL_SORTED_METHOD,
                                 ROO_JPA_ACTIVE_RECORD, new JavaSymbolName(
                                 "findAllSortedMethod"),
                                 FIND_ALL_METHOD_DEFAULT, true, false, "Sorted"),
                        new MethodMatcher(FIND_ENTRIES_SORTED_METHOD,
                                  ROO_JPA_ACTIVE_RECORD, new JavaSymbolName(
                                  "findEntriesSortedMethod"), "find", false,
                                  true, "EntriesSorted"),                                 
                        new MethodMatcher(
                                FIND_METHOD, ROO_JPA_ACTIVE_RECORD,
                                new JavaSymbolName("findMethod"),
                                FIND_METHOD_DEFAULT, false, true),
                        new MethodMatcher(FLUSH_METHOD, ROO_JPA_ACTIVE_RECORD,
                                new JavaSymbolName("flushMethod"),
                                FLUSH_METHOD_DEFAULT), new MethodMatcher(
                                MERGE_METHOD, ROO_JPA_ACTIVE_RECORD,
                                new JavaSymbolName("mergeMethod"),
                                MERGE_METHOD_DEFAULT), new MethodMatcher(
                                PERSIST_METHOD, ROO_JPA_ACTIVE_RECORD,
                                new JavaSymbolName("persistMethod"),
                                PERSIST_METHOD_DEFAULT), new MethodMatcher(
                                REMOVE_METHOD, ROO_JPA_ACTIVE_RECORD,
                                new JavaSymbolName("removeMethod"),
                                REMOVE_METHOD_DEFAULT));
    }
    
    public ConfigurableMetadataProvider getConfigurableMetadataProvider(){
    	// Get all Services implement ConfigurableMetadataProvider interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ConfigurableMetadataProvider.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ConfigurableMetadataProvider) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ConfigurableMetadataProvider on JpaActiveRecordMetadataProviderImpl.");
			return null;
		}
    }
    
    public CustomDataKeyDecorator getCustomDataKeyDecorator(){
    	// Get all Services implement CustomDataKeyDecorator interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(CustomDataKeyDecorator.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (CustomDataKeyDecorator) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load CustomDataKeyDecorator on JpaActiveRecordMetadataProviderImpl.");
			return null;
		}
    }
    
    public PluralMetadataProvider getPluralMetadataProvider(){
    	// Get all Services implement PluralMetadataProvider interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(PluralMetadataProvider.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (PluralMetadataProvider) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load PluralMetadataProvider on JpaActiveRecordMetadataProviderImpl.");
			return null;
		}
    }
    
    public ProjectOperations getProjectOperations(){
    	// Get all Services implement ProjectOperations interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ProjectOperations) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ProjectOperations on JpaActiveRecordMetadataProviderImpl.");
			return null;
		}
    }
    
    
    
}
