package org.springframework.roo.addon.jsf;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link JsfMenuBeanMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true) 
@Service 
public final class JsfMenuBeanMetadataProviderImpl extends AbstractItdMetadataProvider implements JsfMenuBeanMetadataProvider {
	@Reference private TypeLocationService typeLocationService;
	@Reference private ProjectOperations projectOperations;

	// Stores the MID (as accepted by this JsfMenuBeanMetadataProvider) for the one (and only one) application-wide menu bean
	private String menuBeanMid;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(JsfManagedBeanMetadata.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(new JavaType(RooJsfMenuBean.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.deregisterDependency(JsfManagedBeanMetadata.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(new JavaType(RooJsfMenuBean.class.getName()));
	}
	
	@Override
	protected String resolveDownstreamDependencyIdentifier(String upstreamDependency) {
		if (MetadataIdentificationUtils.getMetadataClass(upstreamDependency).equals(MetadataIdentificationUtils.getMetadataClass(JsfManagedBeanMetadata.getMetadataIdentiferType()))) {
			// A JsfManagedBeanMetadata upstream MID has changed or become available for the first time
			// It's OK to return null if we don't yet know the MID because its JavaType has never been found
			return menuBeanMid;
		}
		
		// It wasn't a JsfManagedBeanMetadata, so we can let the superclass handle it
		// (it's expected it would be a PhysicalTypeIdentifier notification, as that's the only other thing we registered to receive)
		return super.resolveDownstreamDependencyIdentifier(upstreamDependency);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		menuBeanMid = metadataIdentificationString;
		
		// To get here we know the governor is the MenuBean so let's go ahead and create its ITD
		Set<ClassOrInterfaceTypeDetails> managedBeans = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooJsfManagedBean.class.getName()));
		for (ClassOrInterfaceTypeDetails managedBean : managedBeans) {
			metadataDependencyRegistry.registerDependency(managedBean.getDeclaredByMetadataId(), metadataIdentificationString);
		}
		
		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		Assert.notNull(projectMetadata, "Project metadata required");

		return new JsfMenuBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, managedBeans, projectMetadata.getProjectName());
	}

	public String getItdUniquenessFilenameSuffix() {
		return "MenuBean";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JsfMenuBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JsfMenuBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JsfMenuBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfMenuBeanMetadata.getMetadataIdentiferType();
	}
}