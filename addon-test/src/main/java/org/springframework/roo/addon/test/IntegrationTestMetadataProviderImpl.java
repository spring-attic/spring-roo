package org.springframework.roo.addon.test;

import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD;

import java.util.LinkedHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.dod.DataOnDemandMetadata;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.layers.LayerService;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link IntegrationTestMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class IntegrationTestMetadataProviderImpl extends AbstractItdMetadataProvider implements IntegrationTestMetadataProvider {
	
	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private LayerService layerService;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		// Integration test classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
		addMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
		removeMetadataTrigger(new JavaType(RooIntegrationTest.class.getName()));
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataId, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null || !projectMetadata.isValid()) {
			return null;
		}
		
		// We need to parse the annotation, which we expect to be present
		IntegrationTestAnnotationValues annotationValues = new IntegrationTestAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType entity = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entity == null) {
			return null;
		}
		
		// Lookup the DOD's metadata. The DOD must conform to the DOD naming conventions
		JavaType dodJavaType = new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
		String dataOnDemandMetadataKey = DataOnDemandMetadata.createIdentifier(dodJavaType, Path.SRC_TEST_JAVA);
		DataOnDemandMetadata dataOnDemandMetadata = (DataOnDemandMetadata) metadataService.get(dataOnDemandMetadataKey);

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(dataOnDemandMetadataKey, metadataId);

		if (dataOnDemandMetadata == null || !dataOnDemandMetadata.isValid()) {
			return null;
		}
		
		// Lookup the entity's metadata
		MemberDetails memberDetails = getMemberDetails(entity);
		
		MethodMetadata identifierAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, IDENTIFIER_ACCESSOR_METHOD);
		MethodMetadata versionAccessorMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, VERSION_ACCESSOR_METHOD);
		MethodMetadata countMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, COUNT_ALL_METHOD);
		MethodMetadata findMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, FIND_METHOD);
		final LinkedHashMap<JavaSymbolName, Object> findAllMethodParams = new LinkedHashMap<JavaSymbolName, Object>();
		final MemberTypeAdditions findAllMethodAdditions = layerService.getMemberTypeAdditions(metadataId, FIND_ALL_METHOD.name(), entity, findAllMethodParams, LayerType.HIGHEST.getPosition());
		MethodMetadata findEntriesMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, FIND_ENTRIES_METHOD);
		MethodMetadata flushMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, FLUSH_METHOD);
		MethodMetadata mergeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, MERGE_METHOD);
		MethodMetadata persistMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PERSIST_METHOD);
		MethodMetadata removeMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, REMOVE_METHOD);
		
		AnnotationMetadata rooEntity = MemberFindingUtils.getDeclaredTypeAnnotation(memberDetails, new JavaType("org.springframework.roo.addon.entity.RooEntity"));
		String transactionManager = null;
		StringAttributeValue transactionManagerAttr = (StringAttributeValue) rooEntity.getAttribute(new JavaSymbolName("transactionManager"));
		if (transactionManagerAttr != null) {
			transactionManager = transactionManagerAttr.getValue();
		}
		
		boolean hasEmbeddedIdentifier = dataOnDemandMetadata.hasEmbeddedIdentifier();
		boolean entityHasSuperclass  = getEntitySuperclass(entity) != null;

		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			if (memberHoldingTypeDetails instanceof ClassOrInterfaceTypeDetails) {
				metadataDependencyRegistry.registerDependency(memberHoldingTypeDetails.getDeclaredByMetadataId(), metadataId);
				break;
			}
		}

		return new IntegrationTestMetadata(metadataId, aspectName, governorPhysicalTypeMetadata, projectMetadata, annotationValues, dataOnDemandMetadata, identifierAccessorMethod, versionAccessorMethod, countMethod, findMethod, findAllMethodAdditions, findEntriesMethod, flushMethod, mergeMethod, persistMethod, removeMethod, transactionManager, hasEmbeddedIdentifier, entityHasSuperclass);
	}
	
	private ClassOrInterfaceTypeDetails getEntitySuperclass(JavaType entity) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(entity, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		PhysicalTypeDetails ptd = ptm.getMemberHoldingTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		Assert.isInstanceOf(ClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(physicalTypeIdentifier));
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) ptd;
		return classOrInterfaceTypeDetails.getSuperclass();
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "IntegrationTest";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = IntegrationTestMetadata.getJavaType(metadataIdentificationString);
		Path path = IntegrationTestMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return IntegrationTestMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return IntegrationTestMetadata.getMetadataIdentiferType();
	}
}