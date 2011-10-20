package org.springframework.roo.addon.entity;

import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.equals.EqualsMetadataProvider;
import org.springframework.roo.addon.serializable.SerializableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of {@link IdentifierMetadataProvider}.
 *
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class IdentifierMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider implements IdentifierMetadataProvider {

	// Fields
	@Reference private ProjectOperations projectOperations;
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private EqualsMetadataProvider equalsMetadataProvider;
	@Reference private SerializableMetadataProvider serializableMetadataProvider;

	private boolean noArgConstructor = true;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_IDENTIFIER);
		configurableMetadataProvider.addMetadataTrigger(ROO_IDENTIFIER);
		equalsMetadataProvider.addMetadataTrigger(ROO_IDENTIFIER);
		serializableMetadataProvider.addMetadataTrigger(ROO_IDENTIFIER);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_IDENTIFIER);
		configurableMetadataProvider.removeMetadataTrigger(ROO_IDENTIFIER);
		equalsMetadataProvider.removeMetadataTrigger(ROO_IDENTIFIER);
		serializableMetadataProvider.removeMetadataTrigger(ROO_IDENTIFIER);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		JavaType javaType = IdentifierMetadata.getJavaType(metadataIdentificationString);
		List<Identifier> identifierServiceResult = getIdentifiersForType(javaType);

		ContextualPath path = PhysicalTypeIdentifierNamingUtils.getPath(metadataIdentificationString);
		if (projectOperations.isProjectAvailable(path.getModule())) {
			// If the project itself changes, we want a chance to refresh this item
			metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(path.getModule()), metadataIdentificationString);
		}
		
		final IdentifierAnnotationValues annotationValues = new IdentifierAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		return new IdentifierMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, noArgConstructor, identifierServiceResult, annotationValues);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Identifier";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = IdentifierMetadata.getJavaType(metadataIdentificationString);
		ContextualPath path = IdentifierMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final ContextualPath path) {
		return IdentifierMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return IdentifierMetadata.getMetadataIdentifierType();
	}

	/**
	 * Allows disabling the automated creation of no arg constructors. This might be appropriate, for example, if another add-on is providing more sophisticated constructor creation facilities.
	 *
	 * @param noArgConstructor automatically causes any {@link EntityMetadata} to have a no-arg constructor added if there are zero no-arg constructors defined in the {@link PhysicalTypeMetadata}
	 * (defaults to true).
	 */
	public void setNoArgConstructor(final boolean noArgConstructor) {
		this.noArgConstructor = noArgConstructor;
	}
}
