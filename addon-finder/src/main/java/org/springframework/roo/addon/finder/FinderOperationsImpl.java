package org.springframework.roo.addon.finder;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
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
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link FinderOperations}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class FinderOperationsImpl implements FinderOperations {

	// Constants
	private static final Logger logger = HandlerUtils.getLogger(FinderOperationsImpl.class);

	// Fields
	@Reference private DynamicFinderServices dynamicFinderServices;
	@Reference private FileManager fileManager;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeManagementService typeManagementService;
	@Reference private TypeLocationService typeLocationService;

	public boolean isFinderCommandAvailable() {
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public SortedSet<String> listFindersFor(final JavaType typeName, final Integer depth) {
		Assert.notNull(typeName, "Java type required");

		String id = typeLocationService.findIdentifier(typeName);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + typeName.getFullyQualifiedTypeName() + "'");
		}

		// Go and get the entity metadata, as any type with finders has to be an entity
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
		Path path = PhysicalTypeIdentifier.getPath(id);
		String entityMid = EntityMetadata.createIdentifier(javaType, path);

		// Get the entity metadata
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMid);
		if (entityMetadata == null) {
			throw new IllegalArgumentException("Cannot provide finders because '" + typeName.getFullyQualifiedTypeName() + "' is not an entity");
		}

		// Get the member details
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			throw new IllegalStateException("Could not determine physical type metadata for type " + javaType);
		}
		ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		if (cid == null) {
			throw new IllegalStateException("Could not determine class or interface type details for type " + javaType);
		}
		final MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), cid);
		final List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(javaType);
		FieldMetadata versionField = persistenceMemberLocator.getVersionField(javaType);

		// Compute the finders (excluding the ID, version, and EM fields)
		Set<JavaSymbolName> exclusions = new HashSet<JavaSymbolName>();
		exclusions.add(entityMetadata.getEntityManagerField().getFieldName());
		for (final FieldMetadata idField : idFields) {
			exclusions.add(idField.getFieldName());
		}

		if (versionField != null) {
			exclusions.add(versionField.getFieldName());
		}

		SortedSet<String> result = new TreeSet<String>();

		List<JavaSymbolName> finders = dynamicFinderServices.getFinders(memberDetails, entityMetadata.getPlural(), depth, exclusions);
		for (JavaSymbolName finder : finders) {
			// Avoid displaying problematic finders
			try {
				QueryHolder queryHolder = dynamicFinderServices.getQueryHolder(memberDetails, finder, entityMetadata.getPlural(), entityMetadata.getEntityName());
				List<JavaSymbolName> parameterNames = queryHolder.getParameterNames();
				List<JavaType> parameterTypes = queryHolder.getParameterTypes();
				StringBuilder signature = new StringBuilder();
				int x = -1;
				for (JavaType param : parameterTypes) {
					x++;
					if (x > 0) {
						signature.append(", ");
					}
					signature.append(param.getSimpleTypeName()).append(" ").append(parameterNames.get(x).getSymbolName());
				}
				result.add(finder.getSymbolName() + "(" + signature + ")" /* query: '" + query + "'"*/);
			} catch (RuntimeException e) {
				result.add(finder.getSymbolName() + " - failure");
			}
		}
		return result;
	}

	public void installFinder(final JavaType typeName, final JavaSymbolName finderName) {
		Assert.notNull(typeName, "Java type required");
		Assert.notNull(finderName, "Finer name required");

		String id = typeLocationService.findIdentifier(typeName);
		if (id == null) {
			logger.warning("Cannot locate source for '" + typeName.getFullyQualifiedTypeName() + "'");
			return;
		}

		// Go and get the entity metadata, as any type with finders has to be an entity
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
		Path path = PhysicalTypeIdentifier.getPath(id);
		String entityMid = EntityMetadata.createIdentifier(javaType, path);

		// Get the entity metadata
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMid);
		if (entityMetadata == null) {
			logger.warning("Cannot provide finders because '" + typeName.getFullyQualifiedTypeName() + "' is not an entity");
			return;
		}

		// We know the file exists, as there's already entity metadata for it
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.getTypeForIdentifier(id);
		if (classOrInterfaceTypeDetails == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		// We know there should be an existing RooEntity annotation
		List<? extends AnnotationMetadata> annotations = classOrInterfaceTypeDetails.getAnnotations();
		AnnotationMetadata rooEntityAnnotation = MemberFindingUtils.getAnnotationOfType(annotations, ROO_JPA_ACTIVE_RECORD);
		if (rooEntityAnnotation == null) {
			logger.warning("Unable to find the entity annotation on '" + typeName.getFullyQualifiedTypeName() + "'");
			return;
		}

		// Confirm they typed a valid finder name
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), classOrInterfaceTypeDetails);
		if (dynamicFinderServices.getQueryHolder(memberDetails, finderName, entityMetadata.getPlural(), entityMetadata.getEntityName()) == null) {
			logger.warning("Finder name '" + finderName.getSymbolName() + "' either does not exist or contains an error");
			return;
		}

		// Make a destination list to store our final attributes
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		List<StringAttributeValue> desiredFinders = new ArrayList<StringAttributeValue>();

		// Copy the existing attributes, excluding the "finder" attribute
		boolean alreadyAdded = false;
		AnnotationAttributeValue<?> val = rooEntityAnnotation.getAttribute(new JavaSymbolName("finders"));
		if (val != null) {
			// Ensure we have an array of strings
			if (!(val instanceof ArrayAttributeValue<?>)) {
				logger.warning(getErrorMsg());
				return;
			}
			ArrayAttributeValue<?> arrayVal = (ArrayAttributeValue<?>) val;
			for (Object o : arrayVal.getValue()) {
				if (!(o instanceof StringAttributeValue)) {
					logger.warning(getErrorMsg());
					return;
				}
				StringAttributeValue sv = (StringAttributeValue) o;
				if (sv.getValue().equals(finderName.getSymbolName())) {
					alreadyAdded = true;
				}
				desiredFinders.add(sv);
			}
		}

		// Add the desired finder to the end
		if (!alreadyAdded) {
			desiredFinders.add(new StringAttributeValue(new JavaSymbolName("ignored"), finderName.getSymbolName()));
		}

		// Now let's add the "finders" attribute
		attributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("finders"), desiredFinders));

		ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(classOrInterfaceTypeDetails);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(ROO_JPA_ACTIVE_RECORD, attributes);
		classOrInterfaceTypeDetailsBuilder.updateTypeAnnotation(annotation.build(), new HashSet<JavaSymbolName>());
		typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
	}

	private String getErrorMsg() {
		return "Annotation " + ROO_JPA_ACTIVE_RECORD.getSimpleTypeName() + " attribute 'finders' must be an array of strings";
	}
}
