package org.springframework.roo.addon.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.serializable.SerializableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdProviderRole;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link IdentifierMetadata}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component(immediate = true)
@Service
@Reference(name = "identifierService", strategy = ReferenceStrategy.EVENT, policy = ReferencePolicy.DYNAMIC, referenceInterface = IdentifierService.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
public class IdentifierMetadataProviderImpl extends AbstractItdMetadataProvider implements IdentifierMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private SerializableMetadataProvider serializableMetadataProvider;
	private Set<IdentifierService> identifierServices = new HashSet<IdentifierService>();

	protected void bindIdentifierService(IdentifierService is) {
		synchronized (identifierServices) {
			identifierServices.add(is);
		}
	}

	protected void unbindIdentifierService(IdentifierService is) {
		synchronized (identifierServices) {
			identifierServices.remove(is);
		}
	}

	private boolean noArgConstructor = true;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooIdentifier.class.getName()));
		serializableMetadataProvider.addMetadataTrigger(new JavaType(RooIdentifier.class.getName()));
		addProviderRole(ItdProviderRole.ACCESSOR_MUTATOR);
		addMetadataTrigger(new JavaType(RooIdentifier.class.getName()));
	}

	protected void deactivate(ComponentContext context) {
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooIdentifier.class.getName()));
		serializableMetadataProvider.removeMetadataTrigger(new JavaType(RooIdentifier.class.getName()));
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		JavaType javaType = IdentifierMetadata.getJavaType(metadataIdentificationString);

		List<Identifier> identifierServiceResult = null;
		synchronized (identifierServices) {
			for (IdentifierService service : identifierServices) {
				identifierServiceResult = service.getIdentifiers(javaType);
				if (identifierServiceResult != null) {
					// Someone has authoritatively indicated the fields for this PK, so we don't need to continue looping
					break;
				}
			}
		}

		return new IdentifierMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, noArgConstructor, identifierServiceResult);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Identifier";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = IdentifierMetadata.getJavaType(metadataIdentificationString);
		Path path = IdentifierMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return IdentifierMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return IdentifierMetadata.getMetadataIdentiferType();
	}

	/**
	 * Allows disabling the automated creation of no arg constructors. This might be appropriate, for example, if another add-on is providing more sophisticated constructor creation facilities.
	 * 
	 * @param noArgConstructor automatically causes any {@link EntityMetadata} to have a no-arg constructor added if there are zero no-arg constructors defined in the {@link PhysicalTypeMetadata}
	 * (defaults to true).
	 */
	public void setNoArgConstructor(boolean noArgConstructor) {
		this.noArgConstructor = noArgConstructor;
	}
}
