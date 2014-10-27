package org.springframework.roo.addon.javabean;

import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

/**
 * Provides {@link JavaBeanMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {

	private final Set<String> producedMids = new LinkedHashSet<String>();

	@Reference
	private ProjectOperations projectOperations;
	private Boolean wasGaeEnabled;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(
				PhysicalTypeIdentifier.getMetadataIdentiferType(),
				getProvidesType());
		addMetadataTrigger(ROO_JAVA_BEAN);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType,
			final LogicalPath path) {
		return JavaBeanMetadata.createIdentifier(javaType, path);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(
				PhysicalTypeIdentifier.getMetadataIdentiferType(),
				getProvidesType());
		removeMetadataTrigger(ROO_JAVA_BEAN);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(
			final String metadataIdentificationString) {
		final JavaType javaType = JavaBeanMetadata
				.getJavaType(metadataIdentificationString);
		final LogicalPath path = JavaBeanMetadata
				.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	private JavaSymbolName getIdentifierAccessorMethodName(
			final FieldMetadata field, final String metadataIdentificationString) {
		final LogicalPath path = PhysicalTypeIdentifier.getPath(field
				.getDeclaredByMetadataId());
		final String moduleNme = path.getModule();
		if (projectOperations.isProjectAvailable(moduleNme)
				|| !projectOperations.isFeatureInstalled(FeatureNames.GAE)) {
			return null;
		}
		// We are not interested if the field is annotated with
		// @javax.persistence.Transient
		for (final AnnotationMetadata annotationMetadata : field
				.getAnnotations()) {
			if (annotationMetadata.getAnnotationType().equals(TRANSIENT)) {
				return null;
			}
		}
		JavaType fieldType = field.getFieldType();
		// If the field is a common collection type we need to get the element
		// type
		if (fieldType.isCommonCollectionType()) {
			if (fieldType.getParameters().isEmpty()) {
				return null;
			}
			fieldType = fieldType.getParameters().get(0);
		}

		final MethodMetadata identifierAccessor = persistenceMemberLocator
				.getIdentifierAccessor(fieldType);
		if (identifierAccessor != null) {
			metadataDependencyRegistry.registerDependency(
					identifierAccessor.getDeclaredByMetadataId(),
					metadataIdentificationString);
			return identifierAccessor.getMethodName();
		}

		return null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "JavaBean";
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(
			final String metadataIdentificationString,
			final JavaType aspectName,
			final PhysicalTypeMetadata governorPhysicalTypeMetadata,
			final String itdFilename) {
		final JavaBeanAnnotationValues annotationValues = new JavaBeanAnnotationValues(
				governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		ClassOrInterfaceTypeDetails currentClassDetails = governorPhysicalTypeMetadata
				.getMemberHoldingTypeDetails();

		final Map<FieldMetadata, JavaSymbolName> declaredFields = new LinkedHashMap<FieldMetadata, JavaSymbolName>();
		for (final FieldMetadata field : currentClassDetails
				.getDeclaredFields()) {
			declaredFields.put(
					field,
					getIdentifierAccessorMethodName(field,
							metadataIdentificationString));
		}

		// In order to handle switching between GAE and JPA produced MIDs need
		// to be remembered so they can be regenerated on JPA <-> GAE switch
		producedMids.add(metadataIdentificationString);

		// Getting implements
		List<JavaType> interfaces = currentClassDetails.getImplementsTypes();
		List<? extends MethodMetadata> interfaceMethods = null;
		if (!interfaces.isEmpty()) {
			for (JavaType currentInterface : interfaces) {
				ClassOrInterfaceTypeDetails currentInterfaceDetails = typeLocationService
						.getTypeDetails(currentInterface);
				if(currentInterfaceDetails != null){
					interfaceMethods = currentInterfaceDetails.getDeclaredMethods();
				}
			}
		}

		return new JavaBeanMetadata(metadataIdentificationString, aspectName,
				governorPhysicalTypeMetadata, annotationValues, declaredFields,
				interfaceMethods);
	}

	public String getProvidesType() {
		return JavaBeanMetadata.getMetadataIdentiferType();
	}

	// We need to notified when ProjectMetadata changes in order to handle JPA
	// <-> GAE persistence changes
	@Override
	protected void notifyForGenericListener(final String upstreamDependency) {
		// If the upstream dependency is null or invalid do not continue
		if (StringUtils.isBlank(upstreamDependency)
				|| !MetadataIdentificationUtils.isValid(upstreamDependency)) {
			return;
		}
		// If the upstream dependency isn't ProjectMetadata do not continue
		if (!ProjectMetadata.isValid(upstreamDependency)) {
			return;
		}
		// If the project isn't valid do not continue
		if (projectOperations.isProjectAvailable(ProjectMetadata
				.getModuleName(upstreamDependency))) {
			final boolean isGaeEnabled = projectOperations
					.isFeatureInstalled(FeatureNames.GAE);
			// We need to determine if the persistence state has changed, we do
			// this by comparing the last known state to the current state
			final boolean hasGaeStateChanged = wasGaeEnabled == null
					|| isGaeEnabled != wasGaeEnabled;
			if (hasGaeStateChanged) {
				wasGaeEnabled = isGaeEnabled;
				for (final String producedMid : producedMids) {
					metadataService.evictAndGet(producedMid);
				}
			}
		}
	}
}
