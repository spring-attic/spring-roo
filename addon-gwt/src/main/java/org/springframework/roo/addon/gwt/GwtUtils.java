package org.springframework.roo.addon.gwt;

import java.util.ArrayList;
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
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Utility methods used in the GWT Add-On.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
public class GwtUtils {

	public static final String PROXY_REQUEST_WARNING = "// WARNING: THIS FILE IS MANAGED BY SPRING ROO.\n\n";
	public static final JavaType ENTITY_PROXY = new JavaType("com.google.web.bindery.requestfactory.shared.EntityProxy");
	public static final JavaType REQUEST_CONTEXT = new JavaType("com.google.web.bindery.requestfactory.shared.RequestContext");
	public static final JavaType REQUEST = new JavaType("com.google.web.bindery.requestfactory.shared.Request");
	public static final JavaType INSTANCE_REQUEST = new JavaType("com.google.web.bindery.requestfactory.shared.InstanceRequest");
	public static final JavaType PROXY_FOR_NAME = new JavaType("com.google.web.bindery.requestfactory.shared.ProxyForName");
	public static final JavaType PROXY_FOR = new JavaType("com.google.web.bindery.requestfactory.shared.ProxyFor");
	public static final JavaType SERVICE_NAME = new JavaType("com.google.web.bindery.requestfactory.shared.ServiceName");
	public static final JavaType SERVICE = new JavaType("com.google.web.bindery.requestfactory.shared.Service");
	static final JavaType[] PROXY_ANNOTATIONS = {PROXY_FOR, PROXY_FOR_NAME};
	static final JavaType[] REQUEST_ANNOTATIONS = {SERVICE, SERVICE_NAME};
	static final JavaType[] PROXY_REQUEST_ANNOTATIONS = {PROXY_FOR, PROXY_FOR_NAME, SERVICE, SERVICE_NAME};

	private GwtUtils() {
	}

	public static Map<GwtType, JavaType> getMirrorTypeMap(ProjectMetadata projectMetadata, JavaType governorType) {
		Map<GwtType, JavaType> mirrorTypeMap = new HashMap<GwtType, JavaType>();
		for (GwtType mirrorType : GwtType.values()) {
			mirrorTypeMap.put(mirrorType, convertGovernorTypeNameIntoKeyTypeName(governorType, mirrorType, projectMetadata));
		}
		return mirrorTypeMap;
	}

	public static JavaType convertGovernorTypeNameIntoKeyTypeName(JavaType governorType, GwtType type, ProjectMetadata projectMetadata) {
		String destinationPackage = type.getPath().packageName(projectMetadata);
		String typeName;
		if (type.isMirrorType()) {
			String simple = governorType.getSimpleTypeName();
			typeName = destinationPackage + "." + simple + type.getSuffix();
		} else {
			typeName = destinationPackage + "." + type.getTemplate();
		}
		return new JavaType(typeName);
	}

	public static JavaType lookupProxyTargetType(ClassOrInterfaceTypeDetails cid) {
		return lookupTargetType(cid, true);
	}

	public static JavaType lookupRequestTargetType(ClassOrInterfaceTypeDetails cid) {
		return lookupTargetType(cid, false);
	}

	public static JavaType lookupTargetType(ClassOrInterfaceTypeDetails cid, boolean proxy) {
		JavaType stringBasedAnnotation = SERVICE_NAME;
		JavaType classBasedAnnotation = SERVICE;
		if (proxy) {
			stringBasedAnnotation = PROXY_FOR_NAME;
			classBasedAnnotation = PROXY_FOR;
		}
		AnnotationMetadata serviceNameAnnotation = MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), stringBasedAnnotation);
		if (serviceNameAnnotation != null) {
			StringAttributeValue serviceNameAttributeValue = (StringAttributeValue) serviceNameAnnotation.getAttribute("value");
			if (serviceNameAttributeValue != null) {
				return new JavaType(serviceNameAttributeValue.getValue());
			}
		}

		serviceNameAnnotation = MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), classBasedAnnotation);
		if (serviceNameAnnotation != null) {
			ClassAttributeValue serviceAttributeValue = (ClassAttributeValue) serviceNameAnnotation.getAttribute("value");
			if (serviceAttributeValue != null) {
				return serviceAttributeValue.getValue();
			}
		}

		return null;
	}

	public static List<String> getAnnotationValues(ClassOrInterfaceTypeDetails target, JavaType annotationType, String attributeName) {
		List<String> values = new ArrayList<String>();
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(target.getAnnotations(), annotationType);
		if (annotation == null) {
			return values;
		}
		AnnotationAttributeValue<?> attributeValue = annotation.getAttribute(attributeName);
		if (attributeValue != null && attributeValue instanceof ArrayAttributeValue) {
			ArrayAttributeValue<StringAttributeValue> arrayAttributeValue = (ArrayAttributeValue<StringAttributeValue>) attributeValue;
			for (StringAttributeValue value : arrayAttributeValue.getValue()) {
				values.add(value.getValue());
			}
		} else if (attributeValue != null && attributeValue instanceof StringAttributeValue) {
			StringAttributeValue stringAttributeVale = (StringAttributeValue) attributeValue;
			values.add(stringAttributeVale.getValue());
		}
		return values;
	}

	public static boolean getBooleanAnnotationValue(ClassOrInterfaceTypeDetails target, JavaType annotationType, String attributeName, boolean valueIfNull) {
		AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(target.getAnnotations(), annotationType);
		if (annotation == null) {
			return valueIfNull;
		}
		AnnotationAttributeValue<?> attributeValue = annotation.getAttribute(attributeName);
		if (attributeValue != null && attributeValue instanceof BooleanAttributeValue) {
			BooleanAttributeValue booleanAttributeValue = (BooleanAttributeValue) attributeValue;
			return booleanAttributeValue.getValue();
		}
		return valueIfNull;
	}

	public static AnnotationMetadata getFirstAnnotation(ClassOrInterfaceTypeDetails cid, JavaType... annotationTypes) {
		for (JavaType annotationType : annotationTypes) {
			AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), annotationType);
			if (annotationMetadata != null) {
				return annotationMetadata;
			}
		}
		return null;
	}

	public static String getStringValue(AnnotationAttributeValue<?> attributeValue) {
		if (attributeValue instanceof StringAttributeValue) {
			return ((StringAttributeValue)attributeValue).getValue();
		} else if (attributeValue instanceof ClassAttributeValue) {
			return ((ClassAttributeValue)attributeValue).getValue().getFullyQualifiedTypeName();
		}
		return null;
	}
}
