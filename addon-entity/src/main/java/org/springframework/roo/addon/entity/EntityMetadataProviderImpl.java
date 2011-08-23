package org.springframework.roo.addon.entity;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
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
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

import java.util.List;

import static org.springframework.roo.addon.entity.RooEntity.CLEAR_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.COUNT_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.FIND_ALL_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.FIND_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.FLUSH_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.MERGE_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.PERSIST_METHOD_DEFAULT;
import static org.springframework.roo.addon.entity.RooEntity.REMOVE_METHOD_DEFAULT;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.RooJavaType.ROO_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;

/**
 * Implementation of {@link EntityMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class EntityMetadataProviderImpl extends AbstractItdMetadataProvider implements EntityMetadataProvider {
	
	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private PluralMetadataProvider pluralMetadataProvider;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_ENTITY);
		configurableMetadataProvider.addMetadataTrigger(ROO_ENTITY);
		pluralMetadataProvider.addMetadataTrigger(ROO_ENTITY);
		registerMatchers();
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_ENTITY);
		configurableMetadataProvider.removeMetadataTrigger(ROO_ENTITY);
		pluralMetadataProvider.removeMetadataTrigger(ROO_ENTITY);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}
	
	@SuppressWarnings("unchecked")
	private void registerMatchers() {
		customDataKeyDecorator.registerMatchers(
				getClass(),
				new MethodMatcher(CLEAR_METHOD, ROO_ENTITY, new JavaSymbolName("clearMethod"), CLEAR_METHOD_DEFAULT),
				new MethodMatcher(COUNT_ALL_METHOD, ROO_ENTITY, new JavaSymbolName("countMethod"), COUNT_METHOD_DEFAULT, true, false),
				new MethodMatcher(FIND_ALL_METHOD, ROO_ENTITY, new JavaSymbolName("findAllMethod"), FIND_ALL_METHOD_DEFAULT, true, false),
				new MethodMatcher(FIND_ENTRIES_METHOD, ROO_ENTITY, new JavaSymbolName("findEntriesMethod"), "find", false, true, "Entries"),
				new MethodMatcher(FIND_METHOD, ROO_ENTITY, new JavaSymbolName("findMethod"), FIND_METHOD_DEFAULT, false, true),
				new MethodMatcher(FLUSH_METHOD, ROO_ENTITY, new JavaSymbolName("flushMethod"), FLUSH_METHOD_DEFAULT),
				new MethodMatcher(MERGE_METHOD, ROO_ENTITY, new JavaSymbolName("mergeMethod"), MERGE_METHOD_DEFAULT),
				new MethodMatcher(PERSIST_METHOD, ROO_ENTITY, new JavaSymbolName("persistMethod"), PERSIST_METHOD_DEFAULT),
				new MethodMatcher(REMOVE_METHOD, ROO_ENTITY, new JavaSymbolName("removeMethod"), REMOVE_METHOD_DEFAULT)
		);
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalType, final String itdFilename) {
		// Get the CRUD-related annotation values
		final JpaCrudAnnotationValues crudAnnotationValues = new JpaCrudAnnotationValues(governorPhysicalType);
		// Get the purely JPA-related annotation values, from @RooJpaEntity if present, otherwise from @RooEntity
		JpaEntityAnnotationValues jpaEntityAnnotationValues = new JpaEntityAnnotationValues(governorPhysicalType, ROO_JPA_ENTITY);
		if (!jpaEntityAnnotationValues.isAnnotationFound()) {
			jpaEntityAnnotationValues = new JpaEntityAnnotationValues(governorPhysicalType, ROO_ENTITY);
			Assert.state(jpaEntityAnnotationValues.isAnnotationFound(), "No @RooJpaEntity or @RooEntity on " + metadataId);
		}

		// Look up the inheritance hierarchy for existing EntityMetadata
		final EntityMetadata parent = getParentMetadata((ClassOrInterfaceTypeDetails) governorPhysicalType.getMemberHoldingTypeDetails());

		//If the parent is null, but the type has a super class it is likely that the we don't have information to proceed
		if (parent == null && ((ClassOrInterfaceTypeDetails) governorPhysicalType.getMemberHoldingTypeDetails()).getSuperclass() != null) {
			//If the superclass is annotated with the Entity trigger annotation then we can be pretty sure that we don't have enough information to proceed
			if (MemberFindingUtils.getAnnotationOfType(governorPhysicalType.getMemberHoldingTypeDetails().getAnnotations(), ROO_ENTITY) != null) {
				return null;
			}
		}
		// We also need the plural
		final JavaType entityType = EntityMetadata.getJavaType(metadataId);
		final Path path = EntityMetadata.getPath(metadataId);
		final String pluralMID = PluralMetadata.createIdentifier(entityType, path);
		final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(pluralMID);
		if (pluralMetadata == null) {
			// Can't acquire the plural
			return null;
		}
		metadataDependencyRegistry.registerDependency(pluralMID, metadataId);
	
		// If the project itself changes, we want a chance to refresh this item
		metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(), metadataId);
		
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		final List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(entityType);
		if (idFields.size() != 1) {
			// The ID field metadata is either unavailable or not stable yet
			return null;
		}
		final String entityName = StringUtils.defaultIfEmpty(jpaEntityAnnotationValues.getEntityName(), entityType.getSimpleTypeName());
		return new EntityMetadata(metadataId, aspectName, governorPhysicalType, parent, projectMetadata, crudAnnotationValues, pluralMetadata.getPlural(), idFields.get(0), entityName);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Entity";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = EntityMetadata.getJavaType(metadataIdentificationString);
		Path path = EntityMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return EntityMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return EntityMetadata.getMetadataIdentifierType();
	}

	public JpaCrudAnnotationValues getAnnotationValues(final JavaType javaType) {
		Assert.notNull(javaType, "JavaType required");
		final MemberHoldingTypeDetailsMetadataItem<?> governor = (MemberHoldingTypeDetailsMetadataItem<?>) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType));
		if (MemberFindingUtils.getAnnotationOfType(governor, ROO_ENTITY) == null) {
			// The type can't be found or it's not annotated with @RooEntity
			return null;
		}
		return new JpaCrudAnnotationValues(governor);
	}
}
