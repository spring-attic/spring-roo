package org.springframework.roo.addon.finder;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.activerecord.JpaActiveRecordMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link FinderMetadataProvider}.
 *
 * @author Stefan Schmidt
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class FinderMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements FinderMetadataProvider {

	// Fields
	@Reference private DynamicFinderServices dynamicFinderServices;

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JPA_ACTIVE_RECORD);
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		// We know governor type details are non-null and can be safely cast

		// Work out the MIDs of the other metadata we depend on
		JavaType javaType = FinderMetadata.getJavaType(metadataIdentificationString);
		LogicalPath path = FinderMetadata.getPath(metadataIdentificationString);
		String jpaActiveRecordMetadataKey = JpaActiveRecordMetadata.createIdentifier(javaType, path);

		// We need to lookup the metadata we depend on
		JpaActiveRecordMetadata jpaActiveRecordMetadata = (JpaActiveRecordMetadata) metadataService.get(jpaActiveRecordMetadataKey);
		if (jpaActiveRecordMetadata == null || !jpaActiveRecordMetadata.isValid()) {
			return null;
		}
		final MethodMetadata entityManagerMethod = jpaActiveRecordMetadata.getEntityManagerMethod();
		if (entityManagerMethod == null) {
			return null;
		}

		MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
		if (memberDetails == null) {
			return null;
		}
		
		final String plural = jpaActiveRecordMetadata.getPlural();
		final String entityName = jpaActiveRecordMetadata.getEntityName();

		// Using SortedMap to ensure that the ITD emits finders in the same order each time
		SortedMap<JavaSymbolName, QueryHolder> queryHolders = new TreeMap<JavaSymbolName, QueryHolder>();
		for (String methodName : jpaActiveRecordMetadata.getDynamicFinders()) {
			JavaSymbolName finderName = new JavaSymbolName(methodName);
			QueryHolder queryHolder = dynamicFinderServices.getQueryHolder(memberDetails, finderName, plural, entityName);
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
		metadataDependencyRegistry.registerDependency(jpaActiveRecordMetadataKey, metadataIdentificationString);

		// We make the queryHolders immutable in case FinderMetadata in the future makes it available through an accessor etc
		return new FinderMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, entityManagerMethod, Collections.unmodifiableSortedMap(queryHolders));
	}

	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		return getLocalMid(itdTypeDetails);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Finder";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		JavaType javaType = FinderMetadata.getJavaType(metadataIdentificationString);
		LogicalPath path = FinderMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
		return FinderMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return FinderMetadata.getMetadataIdentiferType();
	}
}
