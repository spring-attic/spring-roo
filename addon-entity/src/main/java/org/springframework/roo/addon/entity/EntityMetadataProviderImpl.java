package org.springframework.roo.addon.entity;

import static org.springframework.roo.addon.entity.RooJpaActiveRecord.CLEAR_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooJpaActiveRecord.COUNT_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooJpaActiveRecord.FIND_ALL_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooJpaActiveRecord.FIND_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooJpaActiveRecord.FLUSH_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooJpaActiveRecord.MERGE_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooJpaActiveRecord.PERSIST_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooJpaActiveRecord.REMOVE_METHOD_DEFAULT;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.displaystring.DisplayStringMetadataProvider;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.plural.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link EntityMetadataProvider}.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class EntityMetadataProviderImpl extends AbstractItdMetadataProvider implements EntityMetadataProvider {

	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	@Reference private DisplayStringMetadataProvider displayStringMetadataProvider;
	@Reference private PluralMetadataProvider pluralMetadataProvider;
	@Reference private ProjectOperations projectOperations;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		configurableMetadataProvider.addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		pluralMetadataProvider.addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		displayStringMetadataProvider.addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		registerMatchers();
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		configurableMetadataProvider.removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		pluralMetadataProvider.removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		displayStringMetadataProvider.removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}

	@SuppressWarnings("unchecked")
	private void registerMatchers() {
		customDataKeyDecorator.registerMatchers(
			getClass(),
			new MethodMatcher(CLEAR_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("clearMethod"), CLEAR_METHOD_DEFAULT),
			new MethodMatcher(COUNT_ALL_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("countMethod"), COUNT_METHOD_DEFAULT, true, false),
			new MethodMatcher(FIND_ALL_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("findAllMethod"), FIND_ALL_METHOD_DEFAULT, true, false),
			new MethodMatcher(FIND_ENTRIES_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("findEntriesMethod"), "find", false, true, "Entries"),
			new MethodMatcher(FIND_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("findMethod"), FIND_METHOD_DEFAULT, false, true),
			new MethodMatcher(FLUSH_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("flushMethod"), FLUSH_METHOD_DEFAULT),
			new MethodMatcher(MERGE_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("mergeMethod"), MERGE_METHOD_DEFAULT),
			new MethodMatcher(PERSIST_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("persistMethod"), PERSIST_METHOD_DEFAULT),
			new MethodMatcher(REMOVE_METHOD, ROO_JPA_ACTIVE_RECORD, new JavaSymbolName("removeMethod"), REMOVE_METHOD_DEFAULT)
		);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalType, final String itdFilename) {
		// Get the CRUD-related annotation values
		final JpaCrudAnnotationValues crudAnnotationValues = new JpaCrudAnnotationValues(governorPhysicalType);
		// Get the purely JPA-related annotation values, from @RooJpaEntity if present, otherwise from @RooJpaActiveRecord
		JpaEntityAnnotationValues jpaEntityAnnotationValues = new JpaEntityAnnotationValues(governorPhysicalType, ROO_JPA_ENTITY);
		if (!jpaEntityAnnotationValues.isAnnotationFound()) {
			jpaEntityAnnotationValues = new JpaEntityAnnotationValues(governorPhysicalType, ROO_JPA_ACTIVE_RECORD);
			Assert.state(jpaEntityAnnotationValues.isAnnotationFound(), "No @RooJpaEntity or @RooJpaActiveRecord on " + metadataId);
		}

		// Look up the inheritance hierarchy for existing EntityMetadata
		final EntityMetadata parent = getParentMetadata((ClassOrInterfaceTypeDetails) governorPhysicalType.getMemberHoldingTypeDetails());

		// If the parent is null, but the type has a super class it is likely that the we don't have information to proceed
		if (parent == null && ((ClassOrInterfaceTypeDetails) governorPhysicalType.getMemberHoldingTypeDetails()).getSuperclass() != null) {
			// If the superclass is annotated with the Entity trigger annotation then we can be pretty sure that we don't have enough information to proceed
			if (MemberFindingUtils.getAnnotationOfType(governorPhysicalType.getMemberHoldingTypeDetails().getAnnotations(), ROO_JPA_ACTIVE_RECORD) != null) {
				return null;
			}
		}
		// We also need the plural
		final JavaType entity = EntityMetadata.getJavaType(metadataId);
		final ContextualPath path = EntityMetadata.getPath(metadataId);
		final String pluralId = PluralMetadata.createIdentifier(entity, path);
		final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralId);
		if (pluralMetadata == null) {
			// Can't acquire the plural
			return null;
		}
		metadataDependencyRegistry.registerDependency(pluralId, metadataId);

		final List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(entity);
		if (idFields.size() != 1) {
			// The ID field metadata is either unavailable or not stable yet
			return null;
		}
		final String entityName = StringUtils.defaultIfEmpty(jpaEntityAnnotationValues.getEntityName(), entity.getSimpleTypeName());

		boolean isGaeEnabled = false;
		boolean isDataNucleusEnabled = false;

		String moduleName = path.getModule();
		if (projectOperations.isProjectAvailable(moduleName)) {
			// If the project itself changes, we want a chance to refresh this item
			metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(moduleName), metadataId);
			isGaeEnabled = projectOperations.isGaeEnabled(moduleName);
			isDataNucleusEnabled = projectOperations.isDataNucleusEnabled(moduleName);
		}
		return new EntityMetadata(metadataId, aspectName, governorPhysicalType, parent, crudAnnotationValues, pluralMetadata.getPlural(), idFields.get(0), entityName, isGaeEnabled, isDataNucleusEnabled);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Jpa_ActiveRecord";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = EntityMetadata.getJavaType(metadataIdentificationString);
		ContextualPath path = EntityMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final ContextualPath path) {
		return EntityMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return EntityMetadata.getMetadataIdentifierType();
	}

	public JpaCrudAnnotationValues getAnnotationValues(final JavaType javaType) {
		Assert.notNull(javaType, "JavaType required");
		final MemberHoldingTypeDetailsMetadataItem<?> governor = (MemberHoldingTypeDetailsMetadataItem<?>) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType));
		if (MemberFindingUtils.getAnnotationOfType(governor, ROO_JPA_ACTIVE_RECORD) == null) {
			// The type can't be found or it's not annotated with @RooJpaActiveRecord
			return null;
		}
		return new JpaCrudAnnotationValues(governor);
	}
}
