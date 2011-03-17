package org.springframework.roo.addon.finder;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Provides {@link FinderMetadata}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class FinderMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements FinderMetadataProvider {
	@Reference private DynamicFinderServices dynamicFinderServices;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// Work out the MIDs of the other metadata we depend on
		JavaType javaType = FinderMetadata.getJavaType(metadataIdentificationString);
		Path path = FinderMetadata.getPath(metadataIdentificationString);
		String entityMetadataKey = EntityMetadata.createIdentifier(javaType, path);
		
		// We need to lookup the metadata we depend on
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMetadataKey);
		if (projectMetadata == null || !projectMetadata.isValid() || entityMetadata == null || !entityMetadata.isValid() || entityMetadata.getEntityManagerMethod() == null) {
			return null;
		}

		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		Assert.notNull(classOrInterfaceTypeDetails, "Governor failed to provide class type details, in violation of superclass contract");
		
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(FinderMetadataProviderImpl.class.getName(), classOrInterfaceTypeDetails);
		
		// Using SortedMap to ensure that if they have > 1 finder the ITD source emits in the same predictable order each finder
		SortedMap<JavaSymbolName, QueryHolder> queryHolders = new TreeMap<JavaSymbolName, QueryHolder>();
		for (String methodName : entityMetadata.getDynamicFinders()) {
			JavaSymbolName finderName = new JavaSymbolName(methodName);
			QueryHolder queryHolder = dynamicFinderServices.getQueryHolder(memberDetails, finderName, entityMetadata.getPlural(), entityMetadata.getEntityName());
			if (queryHolder != null) {
				queryHolders.put(finderName, queryHolder);
			}
		}
		
		// Now determine all the ITDs we're relying on to ensure we are notified if they change
		for (QueryHolder queryHolder : queryHolders.values()) {
			for (Token token : queryHolder.getTokens()) {
				if (token instanceof FieldToken) {
					FieldToken fieldToken = (FieldToken) token;
					String declaredByMid = fieldToken.getField().getDeclaredByMetadataId();
					metadataDependencyRegistry.registerDependency(declaredByMid, metadataIdentificationString);
				}
			}
		}
		
		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(entityMetadataKey, metadataIdentificationString);

		// We make the queryHolders immutable in case FinderMetadata in the future makes it available through an accessor etc
		return new FinderMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, projectMetadata.isDataNucleusEnabled(), entityMetadata.getEntityManagerMethod(), Collections.unmodifiableSortedMap(queryHolders));
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine if this ITD presents a method we're interested in (namely mutators)
		for (MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
			if (BeanInfoUtils.isMutatorMethod(method)) {
				// We care about this ITD, so formally request an update so we can scan for it and process it
				return getLocalMid(itdTypeDetails);
			}
		}
		
		return null;
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Finder";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = FinderMetadata.getJavaType(metadataIdentificationString);
		Path path = FinderMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return FinderMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return FinderMetadata.getMetadataIdentiferType();
	}
}
