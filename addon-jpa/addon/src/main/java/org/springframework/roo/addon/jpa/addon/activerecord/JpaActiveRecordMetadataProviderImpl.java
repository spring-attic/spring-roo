package org.springframework.roo.addon.jpa.addon.activerecord;

import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.CLEAR_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.COUNT_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.FIND_ALL_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.FIND_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.FLUSH_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.MERGE_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.PERSIST_METHOD_DEFAULT;
import static org.springframework.roo.addon.jpa.annotations.activerecord.RooJpaActiveRecord.REMOVE_METHOD_DEFAULT;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_SORTED_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
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
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.addon.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityAnnotationValues;
import org.springframework.roo.addon.plural.addon.PluralMetadata;
import org.springframework.roo.addon.plural.addon.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.customdata.taggers.Matcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProviderTracker;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link JpaActiveRecordMetadataProvider}.
 * 
 * @author Ben Alex
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.0
 */
@Component
@Service
public class JpaActiveRecordMetadataProviderImpl extends
        AbstractItdMetadataProvider implements JpaActiveRecordMetadataProvider {

    protected final static Logger LOGGER = HandlerUtils
            .getLogger(JpaActiveRecordMetadataProviderImpl.class);

    private ProjectOperations projectOperations;

    protected MetadataDependencyRegistryTracker registryTracker = null;
    protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;
    protected ItdTriggerBasedMetadataProviderTracker configurableMetadataProviderTracker = null;
    protected ItdTriggerBasedMetadataProviderTracker pluralMetadataProviderTracker = null;

    /**
     * This service is being activated so setup it:
     * <ul>
     * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
     * <li>Create and open one {@link ItdTriggerBasedMetadataProviderTracker} 
     * for each {@link ConfigurableMetadataProvider} and {@link PluralMetadataProvider}.</li>
     * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
     * <li>Registers {@link RooJavaType#ROO_JPA_ACTIVE_RECORD} as additional 
     * JavaType that will trigger metadata registration.</li>
     * </ul>
     */
    @Override
    protected void activate(final ComponentContext cContext) {
        context = cContext.getBundleContext();
        this.registryTracker = new MetadataDependencyRegistryTracker(context,
                null, PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        this.registryTracker.open();

        addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);

        this.configurableMetadataProviderTracker = new ItdTriggerBasedMetadataProviderTracker(
                context, ConfigurableMetadataProvider.class, ROO_JPA_ACTIVE_RECORD);
        this.configurableMetadataProviderTracker.open();

        this.pluralMetadataProviderTracker = new ItdTriggerBasedMetadataProviderTracker(
                context, PluralMetadataProvider.class, ROO_JPA_ACTIVE_RECORD);
        this.pluralMetadataProviderTracker.open();

        this.keyDecoratorTracker = new CustomDataKeyDecoratorTracker(context,
                getClass(), getMatchers());
        this.keyDecoratorTracker.open();
    }

    /**
     * This service is being deactivated so unregister upstream-downstream 
     * dependencies, triggers, matchers and listeners.
     * 
     * @param context
     */
    protected void deactivate(final ComponentContext context) {
        MetadataDependencyRegistry registry = this.registryTracker.getService();
        registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        this.registryTracker.close();

        removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);

        ItdTriggerBasedMetadataProvider metadataProvider = this.configurableMetadataProviderTracker.getService();
        metadataProvider.removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        this.configurableMetadataProviderTracker.close();

        metadataProvider = this.pluralMetadataProviderTracker.getService();
        metadataProvider.removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
        this.pluralMetadataProviderTracker.close();

        CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
        keyDecorator.unregisterMatchers(getClass());
        this.keyDecoratorTracker.close();
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JpaActiveRecordMetadata.createIdentifier(javaType, path);
    }

    public JpaCrudAnnotationValues getAnnotationValues(final JavaType javaType) {
        Validate.notNull(javaType, "JavaType required");
        final String physicalTypeId = getTypeLocationService()
                .getPhysicalTypeIdentifier(javaType);
        if (StringUtils.isBlank(physicalTypeId)) {
            return null;
        }
        final MemberHoldingTypeDetailsMetadataItem<?> governor = (MemberHoldingTypeDetailsMetadataItem<?>) getMetadataService()
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
        final PluralMetadata pluralMetadata = (PluralMetadata) getMetadataService()
                .get(pluralId);
        if (pluralMetadata == null) {
            // Can't acquire the plural
            return null;
        }
        getMetadataDependencyRegistry().registerDependency(pluralId,
                metadataIdentificationString);

        final List<FieldMetadata> idFields = getPersistenceMemberLocator()
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
        if (getProjectOperations().isProjectAvailable(moduleName)) {
            // If the project itself changes, we want a chance to refresh this
            // item
            getMetadataDependencyRegistry().registerDependency(
                    ProjectMetadata.getProjectIdentifier(moduleName),
                    metadataIdentificationString);
            isGaeEnabled = getProjectOperations().isFeatureInstalledInModule(
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
    private Matcher<? extends CustomDataAccessor>[] getMatchers() {
        Matcher<? extends CustomDataAccessor>[] matchers = new Matcher[] {
                new MethodMatcher(CLEAR_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("clearMethod"), CLEAR_METHOD_DEFAULT),
                new MethodMatcher(COUNT_ALL_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("countMethod"),
                        COUNT_METHOD_DEFAULT, true, false),
                new MethodMatcher(FIND_ALL_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("findAllMethod"),
                        FIND_ALL_METHOD_DEFAULT, true, false),
                new MethodMatcher(FIND_ENTRIES_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("findEntriesMethod"), "find", false,
                        true, "Entries"),
                new MethodMatcher(FIND_ALL_SORTED_METHOD,
                        ROO_JPA_ACTIVE_RECORD, new JavaSymbolName(
                                "findAllSortedMethod"),
                        FIND_ALL_METHOD_DEFAULT, true, false, "Sorted"),
                new MethodMatcher(FIND_ENTRIES_SORTED_METHOD,
                        ROO_JPA_ACTIVE_RECORD, new JavaSymbolName(
                                "findEntriesSortedMethod"), "find", false,
                        true, "EntriesSorted"),
                new MethodMatcher(FIND_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("findMethod"), FIND_METHOD_DEFAULT,
                        false, true),
                new MethodMatcher(FLUSH_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("flushMethod"), FLUSH_METHOD_DEFAULT),
                new MethodMatcher(MERGE_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("mergeMethod"), MERGE_METHOD_DEFAULT),
                new MethodMatcher(PERSIST_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("persistMethod"),
                        PERSIST_METHOD_DEFAULT),
                new MethodMatcher(REMOVE_METHOD, ROO_JPA_ACTIVE_RECORD,
                        new JavaSymbolName("removeMethod"),
                        REMOVE_METHOD_DEFAULT) };
        return matchers;
    }

    protected ProjectOperations getProjectOperations() {
        if (projectOperations == null) {
            // Get all Services implement ProjectOperations interface
            try {
                ServiceReference<?>[] references = context
                        .getAllServiceReferences(
                                ProjectOperations.class.getName(), null);

                for (ServiceReference<?> ref : references) {
                    return (ProjectOperations) context.getService(ref);
                }

                return null;

            }
            catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load ProjectOperations on JpaActiveRecordMetadataProviderImpl.");
                return null;
            }
        }
        else {
            return projectOperations;
        }

    }
}
