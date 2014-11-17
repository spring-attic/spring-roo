package org.springframework.roo.addon.equals;

import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;

import java.lang.reflect.Modifier;
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
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Implementation of {@link EqualsMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class EqualsMetadataProviderImpl extends
        AbstractMemberDiscoveringItdMetadataProvider implements
        EqualsMetadataProvider {

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().addNotificationListener(this);
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_EQUALS);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return EqualsMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().removeNotificationListener(this);
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_EQUALS);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = EqualsMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = EqualsMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Equals";
    }

    @Override
    protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
        return getLocalMid(itdTypeDetails);
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
        final EqualsAnnotationValues annotationValues = new EqualsAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()) {
            return null;
        }

        final MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);
        if (memberDetails == null) {
            return null;
        }

        final JavaType javaType = governorPhysicalTypeMetadata
                .getMemberHoldingTypeDetails().getName();
        final List<FieldMetadata> equalityFields = locateFields(javaType,
                annotationValues.getExcludeFields(), memberDetails,
                metadataIdentificationString);

        return new EqualsMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, annotationValues, equalityFields);
    }

    public String getProvidesType() {
        return EqualsMetadata.getMetadataIdentiferType();
    }

    private List<FieldMetadata> locateFields(final JavaType javaType,
            final String[] excludeFields, final MemberDetails memberDetails,
            final String metadataIdentificationString) {
        final SortedSet<FieldMetadata> locatedFields = new TreeSet<FieldMetadata>(
                new Comparator<FieldMetadata>() {
                    public int compare(final FieldMetadata l,
                            final FieldMetadata r) {
                        return l.getFieldName().compareTo(r.getFieldName());
                    }
                });

        final List<?> excludeFieldsList = CollectionUtils
                .arrayToList(excludeFields);
        final FieldMetadata versionField = getPersistenceMemberLocator()
                .getVersionField(javaType);

        for (final FieldMetadata field : memberDetails.getFields()) {
            if (excludeFieldsList
                    .contains(field.getFieldName().getSymbolName())) {
                continue;
            }
            if (Modifier.isStatic(field.getModifier())
                    || Modifier.isTransient(field.getModifier())
                    || field.getFieldType().isCommonCollectionType()
                    || field.getFieldType().isArray()) {
                continue;
            }
            if (versionField != null
                    && field.getFieldName().equals(versionField.getFieldName())) {
                continue;
            }

            locatedFields.add(field);
            getMetadataDependencyRegistry().registerDependency(
                    field.getDeclaredByMetadataId(),
                    metadataIdentificationString);
        }

        return new ArrayList<FieldMetadata>(locatedFields);
    }
}
