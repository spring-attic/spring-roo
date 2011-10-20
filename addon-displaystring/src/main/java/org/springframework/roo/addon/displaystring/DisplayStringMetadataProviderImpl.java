package org.springframework.roo.addon.displaystring;

import static org.springframework.roo.model.RooJavaType.ROO_DISPLAY_STRING;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;

/**
 * Implementation of  {@link DisplayStringMetadataProvider}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class DisplayStringMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements DisplayStringMetadataProvider {

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_DISPLAY_STRING);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_DISPLAY_STRING);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		final DisplayStringAnnotationValues annotationValues = new DisplayStringAnnotationValues(governorPhysicalTypeMetadata);

		final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
		if (memberDetails == null) {
			return null;
		}

		final JavaType entity = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();
		List<MethodMetadata> locatedAccessors = locateAccessors(entity, memberDetails, metadataIdentificationString);
		if (locatedAccessors.isEmpty()) {
			return null;
		}

		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);

		return new DisplayStringMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, locatedAccessors, identifierAccessor);
	}

	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		return getLocalMid(itdTypeDetails);
	}

	private List<MethodMetadata> locateAccessors(final JavaType entity, final MemberDetails memberDetails, final String metadataIdentificationString) {
		List<MethodMetadata> locatedAccessors = new ArrayList<MethodMetadata>();

		for (final MethodMetadata method : memberDetails.getMethods()) {
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if (method.hasSameName(persistenceMemberLocator.getVersionAccessor(entity))) {
				continue;
			}
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
			if (field == null || isApplicationType(field.getFieldType())) {
				continue;
			}

			metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
			locatedAccessors.add(method);
		}

		return locatedAccessors;
	}

	private boolean isApplicationType(final JavaType javaType) {
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType)) != null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "DisplayString";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = DisplayStringMetadata.getJavaType(metadataIdentificationString);
		ContextualPath path = DisplayStringMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final ContextualPath path) {
		return DisplayStringMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return DisplayStringMetadata.getMetadataIdentiferType();
	}
}
