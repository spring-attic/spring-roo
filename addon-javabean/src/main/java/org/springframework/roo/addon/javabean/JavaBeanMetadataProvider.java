package org.springframework.roo.addon.javabean;

import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides {@link JavaBeanMetadata}.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class JavaBeanMetadataProvider extends AbstractItdMetadataProvider {
	
	// Fields
	@Reference private ProjectOperations projectOperations;
	private final Set<String> producedMids = new LinkedHashSet<String>();
	private Boolean wasGaeEnabled;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JAVA_BEAN);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JAVA_BEAN);
	}

	// We need to notified when ProjectMetadata changes in order to handle JPA <-> GAE persistence changes
	@Override
	protected void notifyForGenericListener(String upstreamDependency) {
		// If the upstream dependency is null or invalid do not continue
		if (!StringUtils.hasText(upstreamDependency) || !MetadataIdentificationUtils.isValid(upstreamDependency)) {
			return;
		}
		// If the upstream dependency isn't ProjectMetadata do not continue
		if (!upstreamDependency.equals(ProjectMetadata.getProjectIdentifier())) {
			return;
		}
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		// If ProjectMetadata isn't valid do not continue
		if (projectMetadata != null && !projectMetadata.isValid()) {
			boolean isGaeEnabled = projectMetadata.isGaeEnabled();
			// We need to determine if the persistence state has changed, we do this by comparing the last known state to the current state
			boolean hasGaeStateChanged = wasGaeEnabled == null || isGaeEnabled != wasGaeEnabled;
			if (hasGaeStateChanged) {
				wasGaeEnabled = isGaeEnabled;
				for (String producedMid : producedMids) {
					metadataService.get(producedMid, true);
				}
			}
		}
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		JavaBeanAnnotationValues annotationValues = new JavaBeanAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		final Map<FieldMetadata, JavaSymbolName> declaredFields = new LinkedHashMap<FieldMetadata, JavaSymbolName>();
		for (FieldMetadata field : governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getDeclaredFields()) {
			declaredFields.put(field, getIdentifierAccessorMethodName(field, metadataIdentificationString));
		}
		
		// In order to handle switching between GAE and JPA produced MIDs need to be remembered so they can be regenerated on JPA <-> GAE switch
		producedMids.add(metadataIdentificationString);
		
		return new JavaBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, declaredFields);
	}

	private JavaSymbolName getIdentifierAccessorMethodName(final FieldMetadata field, String metadataIdentificationString) {
		if (projectOperations.getProjectMetadata() == null || !projectOperations.getProjectMetadata().isGaeEnabled()) {
			return null;
		}
		// We are not interested if the field is annotated with @javax.persistence.Transient
		for (AnnotationMetadata annotationMetadata : field.getAnnotations()) {
			if (annotationMetadata.getAnnotationType().equals(TRANSIENT)) {
				return null;
			}
		}
		JavaType fieldType = field.getFieldType();
		// If the field is a common collection type we need to get the element type
		if (fieldType.isCommonCollectionType()) {
			if (fieldType.getParameters().isEmpty()) {
				return null;
			}
			fieldType = fieldType.getParameters().get(0);
		}

		MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(fieldType);
		if (identifierAccessor != null) {
			metadataDependencyRegistry.registerDependency(identifierAccessor.getDeclaredByMetadataId(), metadataIdentificationString);
			return identifierAccessor.getMethodName();
		}
		
		return null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "JavaBean";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JavaBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JavaBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JavaBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JavaBeanMetadata.getMetadataIdentiferType();
	}
}
