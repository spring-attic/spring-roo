package org.springframework.roo.addon.tostring;

import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link ToStringMetadata}.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class ToStringMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_TO_STRING);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_TO_STRING);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		final ToStringAnnotationValues annotationValues = new ToStringAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
		if (memberDetails == null) {
			return null;
		}

		final JavaType javaType = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getName();
		final List<MethodMetadata> locatedAccessors = locateAccessors(javaType, memberDetails, metadataIdentificationString);
		if (locatedAccessors.isEmpty()) {
			return null;
		}

		return new ToStringMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, locatedAccessors);
	}

	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		return getLocalMid(itdTypeDetails);
	}

	private List<MethodMetadata> locateAccessors(final JavaType javaType, final MemberDetails memberDetails, final String metadataIdentificationString) {
		final SortedSet<MethodMetadata> locatedAccessors = new TreeSet<MethodMetadata>(new Comparator<MethodMetadata>() {
			public int compare(final MethodMetadata l, final MethodMetadata r) {
				return l.getMethodName().compareTo(r.getMethodName());
			}
		});

		for (MethodMetadata method : memberDetails.getMethods()) {
			// Exclude cyclic self-references (ROO-325)
			if (BeanInfoUtils.isAccessorMethod(method) && !method.getReturnType().equals(javaType) && !method.getMethodName().getSymbolName().equals("getDisplayString")) {
				locatedAccessors.add(method);
				// Track any changes to that method (eg it goes away)
				metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
			}
		}
		
		return new ArrayList<MethodMetadata>(locatedAccessors);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ToString";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = ToStringMetadata.getJavaType(metadataIdentificationString);
		Path path = ToStringMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return ToStringMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return ToStringMetadata.getMetadataIdentiferType();
	}
}
