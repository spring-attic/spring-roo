package org.springframework.roo.classpath.scanner;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Default implementation of {@link MemberDetails}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class MemberDetailsImpl implements MemberDetails {

    private final List<MemberHoldingTypeDetails> details = new ArrayList<MemberHoldingTypeDetails>();

    /**
     * Constructs a new instance.
     * 
     * @param details the member holders that should be stored in this instance
     *            (can be <code>null</code>)
     */
    MemberDetailsImpl(
            final Collection<? extends MemberHoldingTypeDetails> details) {
        Validate.notEmpty(details, "Member holding details required");
        CollectionUtils.populate(this.details, details);
    }

    public AnnotationMetadata getAnnotation(final JavaType type) {
        Validate.notNull(type, "Annotation type to locate required");
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            final AnnotationMetadata md = memberHoldingTypeDetails
                    .getAnnotation(type);
            if (md != null) {
                return md;
            }
        }
        return null;
    }

    public List<ConstructorMetadata> getConstructors() {
        final List<ConstructorMetadata> result = new ArrayList<ConstructorMetadata>();
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            result.addAll(memberHoldingTypeDetails.getDeclaredConstructors());
        }
        return result;
    }

    public List<MemberHoldingTypeDetails> getDetails() {
        return Collections.unmodifiableList(details);
    }

    public List<String> getDynamicFinderNames() {
        final List<String> dynamicFinderNames = new ArrayList<String>();
        for (final MemberHoldingTypeDetails mhtd : details) {
            dynamicFinderNames.addAll(mhtd.getDynamicFinderNames());
        }
        return dynamicFinderNames;
    }

    public List<FieldMetadata> getFields() {
        final List<FieldMetadata> result = new ArrayList<FieldMetadata>();
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            result.addAll(memberHoldingTypeDetails.getDeclaredFields());
        }
        return result;
    }

    public MethodMetadata getMethod(final JavaSymbolName methodName) {
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            final MethodMetadata md = MemberFindingUtils.getDeclaredMethod(
                    memberHoldingTypeDetails, methodName);
            if (md != null) {
                return md;
            }
        }
        return null;
    }

    public MethodMetadata getMethod(final JavaSymbolName methodName,
            final List<JavaType> parameters) {
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            final MethodMetadata md = MemberFindingUtils.getDeclaredMethod(
                    memberHoldingTypeDetails, methodName, parameters);
            if (md != null) {
                return md;
            }
        }
        return null;
    }

    public MethodMetadata getMethod(final JavaSymbolName methodName,
            final List<JavaType> parameters, final String excludingMid) {
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            final MethodMetadata method = MemberFindingUtils.getDeclaredMethod(
                    memberHoldingTypeDetails, methodName, parameters);
            if (method != null
                    && !method.getDeclaredByMetadataId().equals(excludingMid)) {
                return method;
            }
        }
        return null;
    }

    public List<MethodMetadata> getMethods() {
        final List<MethodMetadata> result = new ArrayList<MethodMetadata>();
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            result.addAll(memberHoldingTypeDetails.getDeclaredMethods());
        }
        return result;
    }

    public List<MethodMetadata> getMethodsWithTag(final Object tagKey) {
        Validate.notNull(tagKey, "Custom data key required");
        final List<MethodMetadata> result = new ArrayList<MethodMetadata>();
        for (final MethodMetadata method : getMethods()) {
            if (method.getCustomData().keySet().contains(tagKey)) {
                result.add(method);
            }
        }
        return result;
    }

    public MethodMetadata getMostConcreteMethodWithTag(final Object tagKey) {
        return CollectionUtils.firstElementOf(getMethodsWithTag(tagKey));
    }

    public Set<JavaType> getPersistentFieldTypes(final JavaType thisType,
            final PersistenceMemberLocator persistenceMemberLocator) {
        final MethodMetadata identifierAccessor = persistenceMemberLocator
                .getIdentifierAccessor(thisType);
        final MethodMetadata versionAccessor = persistenceMemberLocator
                .getVersionAccessor(thisType);

        final Set<JavaType> fieldTypes = new LinkedHashSet<JavaType>();
        for (final MethodMetadata method : getMethods()) {
            // Not interested in non-accessor methods or persistence identifiers
            // and version fields
            if (!BeanInfoUtils.isAccessorMethod(method)
                    || method.hasSameName(identifierAccessor, versionAccessor)) {
                continue;
            }

            // Not interested in fields that are JPA transient fields or
            // immutable fields
            final FieldMetadata field = BeanInfoUtils
                    .getFieldForJavaBeanMethod(this, method);
            if (field == null
                    || field.getCustomData().keySet()
                            .contains(CustomDataKeys.TRANSIENT_FIELD)
                    || !BeanInfoUtils.hasAccessorAndMutator(field, this)) {
                continue;
            }
            final JavaType returnType = method.getReturnType();
            if (returnType.isCommonCollectionType()) {
                for (final JavaType genericType : returnType.getParameters()) {
                    fieldTypes.add(genericType);
                }
            }
            else {
                if (!field.getCustomData().keySet().contains(EMBEDDED_FIELD)) {
                    fieldTypes.add(returnType);
                }
            }
        }
        return fieldTypes;
    }

    public boolean isMethodDeclaredByAnother(final JavaSymbolName methodName,
            final List<JavaType> parameterTypes,
            final String declaredByMetadataId) {
        final MethodMetadata method = getMethod(methodName, parameterTypes);
        return method != null
                && !method.getDeclaredByMetadataId().equals(
                        declaredByMetadataId);
    }

    public boolean isRequestingAnnotatedWith(
            final AnnotationMetadata annotationMetadata,
            final String requestingMid) {
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : details) {
            if (MemberFindingUtils.getAnnotationOfType(
                    memberHoldingTypeDetails.getAnnotations(),
                    annotationMetadata.getAnnotationType()) != null) {
                if (memberHoldingTypeDetails.getDeclaredByMetadataId().equals(
                        requestingMid)) {
                    return true;
                }
            }
        }
        return false;
    }
}
