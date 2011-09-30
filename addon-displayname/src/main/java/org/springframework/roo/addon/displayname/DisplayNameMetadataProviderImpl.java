package org.springframework.roo.addon.displayname;

import static org.springframework.roo.addon.displayname.RooDisplayName.DISPLAY_NAME_DEFAULT;
import static org.springframework.roo.model.RooJavaType.ROO_DISPLAY_NAME;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Implementation of  {@link DisplayNameMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public final class DisplayNameMetadataProviderImpl extends AbstractItdMetadataProvider implements DisplayNameMetadataProvider {

	// Fields
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_DISPLAY_NAME);
		registerMatchers();
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_DISPLAY_NAME);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}
	
	@SuppressWarnings("unchecked")
	private void registerMatchers() {
		customDataKeyDecorator.registerMatchers(
			getClass(),
			new MethodMatcher(CustomDataKeys.DISPLAY_NAME_METHOD, ROO_DISPLAY_NAME, new JavaSymbolName("methodName"), DISPLAY_NAME_DEFAULT)
		);
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		final DisplayNameAnnotationValues annotationValues = new DisplayNameAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
		if (memberDetails == null) {
			return null;
		}

		final List<MethodMetadata> methods = memberDetails.getMethods();
		if (methods.isEmpty()) {
			return null;
		}

		final JavaType entity = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();
		List<MethodMetadata> locatedAccessors = locateAccessors(entity, memberDetails, metadataIdentificationString);
		if (locatedAccessors.isEmpty()) {
			return null;
		}

		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);

		return new DisplayNameMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, locatedAccessors, identifierAccessor);
	}
	
	private List<MethodMetadata> locateAccessors(final JavaType entity, final MemberDetails memberDetails, final String metadataIdentificationString) {
		final SortedSet<MethodMetadata> locatedAccessors = new TreeSet<MethodMetadata>(new Comparator<MethodMetadata>() {
			public int compare(MethodMetadata l, MethodMetadata r) {
				return l.getMethodName().compareTo(r.getMethodName());
			}
		});

		for (final MethodMetadata method : memberDetails.getMethods()) {
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if (method.hasSameName(persistenceMemberLocator.getVersionAccessor(entity))) {
				continue;
			}
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
			if (field == null) {
				continue;
			}
			if (isApplicationType(field.getFieldType())) {
				continue;
			}
			
			metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
			locatedAccessors.add(method);
		}
		
		return new ArrayList<MethodMetadata>(locatedAccessors);
	}

	private boolean isApplicationType(final JavaType javaType) {
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType)) != null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "DisplayName";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DisplayNameMetadata.getJavaType(metadataIdentificationString);
		Path path = DisplayNameMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DisplayNameMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return DisplayNameMetadata.getMetadataIdentiferType();
	}
}
