package org.springframework.roo.addon.gwt;

import static org.springframework.roo.addon.gwt.GwtJavaType.PROXY_FOR;
import static org.springframework.roo.addon.gwt.GwtJavaType.PROXY_FOR_NAME;
import static org.springframework.roo.addon.gwt.GwtJavaType.RECEIVER;
import static org.springframework.roo.addon.gwt.GwtJavaType.SERVICE;
import static org.springframework.roo.addon.gwt.GwtJavaType.SERVICE_NAME;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_MIRRORED_FROM;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_PROXY;
import static org.springframework.roo.model.RooJavaType.ROO_GWT_REQUEST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;

/**
 * Utility methods used in the GWT Add-On.
 * 
 * @author James Tyrrell
 * @since 1.1.2
 */
public final class GwtUtils {

    public static final JavaType[] PROXY_ANNOTATIONS = { PROXY_FOR,
            PROXY_FOR_NAME };
    public static final String PROXY_REQUEST_WARNING = "// WARNING: THIS FILE IS MANAGED BY SPRING ROO.\n\n";
    public static final JavaType[] REQUEST_ANNOTATIONS = { SERVICE,
            SERVICE_NAME };
    public static final JavaType[] ROO_PROXY_REQUEST_ANNOTATIONS = {
            ROO_GWT_PROXY, ROO_GWT_REQUEST, ROO_GWT_MIRRORED_FROM };

    public static JavaType convertGovernorTypeNameIntoKeyTypeName(
            final JavaType governorType, final GwtType type,
            final JavaPackage topLevelPackage) {
        final String destinationPackage = type.getPath().packageName(
                topLevelPackage);
        String typeName;
        if (type.isMirrorType()) {
            final String simple = governorType.getSimpleTypeName();
            typeName = destinationPackage + "." + simple + type.getSuffix();
        }
        else {
            typeName = destinationPackage + "." + type.getTemplate();
        }
        return new JavaType(typeName);
    }

    public static JavaType convertPrimitiveType(final JavaType type,
            final boolean convertVoid) {
        if (!convertVoid && JavaType.VOID_PRIMITIVE.equals(type)) {
            return type;
        }
        if (type != null && type.isPrimitive()) {
            return new JavaType(type.getFullyQualifiedTypeName());
        }
        return type;
    }

    public static List<String> getAnnotationValues(
            final ClassOrInterfaceTypeDetails target,
            final JavaType annotationType, final String attributeName) {
        final List<String> values = new ArrayList<String>();
        final AnnotationMetadata annotation = MemberFindingUtils
                .getAnnotationOfType(target.getAnnotations(), annotationType);
        if (annotation == null) {
            return values;
        }
        final AnnotationAttributeValue<?> attributeValue = annotation
                .getAttribute(attributeName);
        if (attributeValue != null
                && attributeValue instanceof ArrayAttributeValue) {
            @SuppressWarnings("unchecked")
            final ArrayAttributeValue<StringAttributeValue> arrayAttributeValue = (ArrayAttributeValue<StringAttributeValue>) attributeValue;
            for (final StringAttributeValue value : arrayAttributeValue
                    .getValue()) {
                values.add(value.getValue());
            }
        }
        else if (attributeValue != null
                && attributeValue instanceof StringAttributeValue) {
            final StringAttributeValue stringAttributeVale = (StringAttributeValue) attributeValue;
            values.add(stringAttributeVale.getValue());
        }
        return values;
    }

    public static boolean getBooleanAnnotationValue(
            final ClassOrInterfaceTypeDetails target,
            final JavaType annotationType, final String attributeName,
            final boolean valueIfNull) {
        final AnnotationMetadata annotation = MemberFindingUtils
                .getAnnotationOfType(target.getAnnotations(), annotationType);
        if (annotation == null) {
            return valueIfNull;
        }
        final AnnotationAttributeValue<?> attributeValue = annotation
                .getAttribute(attributeName);
        if (attributeValue != null
                && attributeValue instanceof BooleanAttributeValue) {
            final BooleanAttributeValue booleanAttributeValue = (BooleanAttributeValue) attributeValue;
            return booleanAttributeValue.getValue();
        }
        return valueIfNull;
    }

    public static AnnotationMetadata getFirstAnnotation(
            final ClassOrInterfaceTypeDetails cid,
            final JavaType... annotationTypes) {
        for (final JavaType annotationType : annotationTypes) {
            final AnnotationMetadata annotationMetadata = MemberFindingUtils
                    .getAnnotationOfType(cid.getAnnotations(), annotationType);
            if (annotationMetadata != null) {
                return annotationMetadata;
            }
        }
        return null;
    }

    public static Map<GwtType, JavaType> getMirrorTypeMap(
            final JavaType governorType, final JavaPackage topLevelPackage) {
        final Map<GwtType, JavaType> mirrorTypeMap = new HashMap<GwtType, JavaType>();
        for (final GwtType mirrorType : GwtType.values()) {
            mirrorTypeMap.put(
                    mirrorType,
                    convertGovernorTypeNameIntoKeyTypeName(governorType,
                            mirrorType, topLevelPackage));
        }
        return mirrorTypeMap;
    }

    /**
     * Returns the {@link #RECEIVER} Java type, generically typed to the given
     * type.
     * 
     * @param genericType (required)
     * @return a non-<code>null</code> type
     */
    public static JavaType getReceiverType(final JavaType genericType) {
        return new JavaType(RECEIVER.getFullyQualifiedTypeName(), 0,
                DataType.TYPE, null, Collections.singletonList(genericType));
    }

    public static String getStringValue(
            final AnnotationAttributeValue<?> attributeValue) {
        if (attributeValue instanceof StringAttributeValue) {
            return ((StringAttributeValue) attributeValue).getValue();
        }
        else if (attributeValue instanceof ClassAttributeValue) {
            return ((ClassAttributeValue) attributeValue).getValue()
                    .getFullyQualifiedTypeName();
        }
        return null;
    }

    public static JavaType lookupProxyTargetType(
            final ClassOrInterfaceTypeDetails proxyType) {
        return lookupTargetType(proxyType, PROXY_FOR, PROXY_FOR_NAME);
    }

    public static JavaType lookupRequestTargetType(
            final ClassOrInterfaceTypeDetails requestType) {
        return lookupTargetType(requestType, SERVICE, SERVICE_NAME);
    }

    private static JavaType lookupTargetType(
            final ClassOrInterfaceTypeDetails annotatedType,
            final JavaType classBasedAnnotationType,
            final JavaType stringBasedAnnotationType) {
        final AnnotationMetadata stringBasedAnnotation = annotatedType
                .getAnnotation(stringBasedAnnotationType);
        if (stringBasedAnnotation != null) {
            final AnnotationAttributeValue<String> targetTypeAttributeValue = stringBasedAnnotation
                    .getAttribute("value");
            if (targetTypeAttributeValue != null) {
                return new JavaType(targetTypeAttributeValue.getValue());
            }
        }

        final AnnotationMetadata classBasedAnnotation = annotatedType
                .getAnnotation(classBasedAnnotationType);
        if (classBasedAnnotation != null) {
            final AnnotationAttributeValue<JavaType> targetTypeAttributeValue = classBasedAnnotation
                    .getAttribute("value");
            if (targetTypeAttributeValue != null) {
                return targetTypeAttributeValue.getValue();
            }
        }

        return null;
    }

    public static boolean scaffoldProxy(final ClassOrInterfaceTypeDetails proxy) {
        return GwtUtils.getBooleanAnnotationValue(proxy,
                RooJavaType.ROO_GWT_PROXY, "scaffold", false);
    }

    /**
     * Constructor is private to prevent instantiation
     */
    private GwtUtils() {
    }
}
