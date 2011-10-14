package org.springframework.roo.addon.jsf.application;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_APPLICATION_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata;
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
 * Implementation of {@link JsfApplicationBeanMetadataProvider}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class JsfApplicationBeanMetadataProviderImpl extends AbstractItdMetadataProvider implements JsfApplicationBeanMetadataProvider {
	
	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private TypeLocationService typeLocationService;
	@Reference private ProjectOperations projectOperations;

	// Stores the MID (as accepted by this JsfApplicationBeanMetadataProvider) for the one (and only one) application-wide menu bean
	private String menuBeanMid;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.registerDependency(JsfManagedBeanMetadata.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
		configurableMetadataProvider.addMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		metadataDependencyRegistry.deregisterDependency(JsfManagedBeanMetadata.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
		configurableMetadataProvider.removeMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
	}

	@Override
	protected String resolveDownstreamDependencyIdentifier(final String upstreamDependency) {
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
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		menuBeanMid = metadataIdentificationString;

		// To get here we know the governor is the MenuBean so let's go ahead and create its ITD
		Set<ClassOrInterfaceTypeDetails> managedBeans = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_JSF_MANAGED_BEAN);
		for (ClassOrInterfaceTypeDetails managedBean : managedBeans) {
			metadataDependencyRegistry.registerDependency(managedBean.getDeclaredByMetadataId(), metadataIdentificationString);
		}

		ProjectMetadata projectMetadata = projectOperations.getProjectMetadata();
		Assert.notNull(projectMetadata, "Project metadata required");

		return new JsfApplicationBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, managedBeans, projectMetadata.getProjectName());
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ApplicationBean";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = JsfApplicationBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JsfApplicationBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return JsfApplicationBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfApplicationBeanMetadata.getMetadataIdentiferType();
	}
}