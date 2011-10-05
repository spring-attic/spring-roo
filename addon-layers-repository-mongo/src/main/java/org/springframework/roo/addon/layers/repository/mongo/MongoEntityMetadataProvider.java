package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_MUTATOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.model.RooJavaType.ROO_MONGO_ENTITY;

import java.util.Arrays;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.AnnotatedTypeMatcher;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.Path;

/**
 * Provides the metadata for an ITD that implements a Spring Data Mongo domain entity
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class MongoEntityMetadataProvider extends AbstractItdMetadataProvider {

	// Constants
	private static final FieldMatcher ID_FIELD_MATCHER = new FieldMatcher(IDENTIFIER_FIELD, AnnotationMetadataBuilder.getInstance(SpringJavaType.DATA_ID.getFullyQualifiedTypeName()));
	private static final AnnotatedTypeMatcher PERSISTENT_TYPE_MATCHER = new AnnotatedTypeMatcher(PERSISTENT_TYPE, RooJavaType.ROO_MONGO_ENTITY);
	private static final MethodMatcher ID_ACCESSOR_MATCHER = new MethodMatcher(Arrays.asList(ID_FIELD_MATCHER), IDENTIFIER_ACCESSOR_METHOD, true);
	private static final MethodMatcher ID_MUTATOR_MATCHER = new MethodMatcher(Arrays.asList(ID_FIELD_MATCHER), IDENTIFIER_MUTATOR_METHOD, false);

	// Fields
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;

	@SuppressWarnings("unchecked")
	protected void activate(final ComponentContext context) {
		super.setDependsOnGovernorBeingAClass(false);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_MONGO_ENTITY);
		customDataKeyDecorator.registerMatchers(getClass(), PERSISTENT_TYPE_MATCHER, ID_FIELD_MATCHER, ID_ACCESSOR_MATCHER, ID_MUTATOR_MATCHER);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_MONGO_ENTITY);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		final MongoEntityAnnotationValues annotationValues = new MongoEntityAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType idType = annotationValues.getIdentifierType();
		if (!annotationValues.isAnnotationFound() || idType == null) {
			return null;
		}

		// Get the governor's members
		final MemberDetails governorMemberDetails = getMemberDetails(governorPhysicalTypeMetadata);

		return new MongoEntityMetadata(metadataId, aspectName, governorPhysicalTypeMetadata, idType, governorMemberDetails);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Mongo_Entity";
	}

	public String getProvidesType() {
		return MongoEntityMetadata.getMetadataIdentiferType();
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return MongoEntityMetadata.createIdentifier(javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = MongoEntityMetadata.getJavaType(metadataIdentificationString);
		final Path path = MongoEntityMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}
}
