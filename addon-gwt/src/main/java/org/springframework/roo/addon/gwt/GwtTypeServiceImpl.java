package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.StringUtils;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

/**
 * Provides a basic implementation of {@link GwtTypeService}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
@Component
@Service
public class GwtTypeServiceImpl implements GwtTypeService {
	@Reference private MetadataService metadataService;
	@Reference private MemberDetailsScanner memberDetailsScanner;

	private static Logger logger = HandlerUtils.getLogger(GwtTypeServiceImpl.class);

	/**
	 * Return the type arg for the client side method, given the domain method return type.
	 * If domainMethodReturnType is List<Integer> or Set<Integer>, returns the same.
	 * If domainMethodReturnType is List<Employee>, return List<EmployeeProxy>
	 *
	 * @param type
	 * @param projectMetadata
	 * @param governorType
	 * @return the GWT side leaf type as a JavaType
	 */
	public JavaType getGwtSideLeafType(JavaType type, ProjectMetadata projectMetadata, JavaType governorType, boolean requestType) {
		if (type.isPrimitive()) {
			if (!requestType) {
				GwtUtils.checkPrimitive(type);
			}
			return GwtUtils.convertPrimitiveType(type);
		}

		if (GwtUtils.isCommonType(type)) {
			return type;
		}

		if (GwtUtils.isCollectionType(type)) {
			List<JavaType> args = type.getParameters();
			if (args != null && args.size() == 1) {
				JavaType elementType = args.get(0);
				return new JavaType(type.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(getGwtSideLeafType(elementType, projectMetadata, governorType, requestType)));
			}
			return type;
		}

		PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));

		if (GwtUtils.isDomainObject(type, ptmd)) {
			if (GwtUtils.isEmbeddable(ptmd)) {
				throw new IllegalStateException("GWT does not currently support embedding objects in entities, such as '" + type.getSimpleTypeName() + "' in '" + governorType.getSimpleTypeName() + "'.");
			}
			return GwtUtils.getDestinationJavaType(type, GwtType.PROXY, projectMetadata);
		}

		return type;
	}

	public List<MemberHoldingTypeDetails> getExtendsTypes(ClassOrInterfaceTypeDetails childType) {
		List<MemberHoldingTypeDetails> extendsTypes = new ArrayList<MemberHoldingTypeDetails>();
		if (childType != null) {
			for (JavaType javaType : childType.getExtendsTypes()) {
				String superTypeId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
				if (metadataService.get(superTypeId) == null) {
					continue;
				}
				MemberHoldingTypeDetails superType = ((PhysicalTypeMetadata) metadataService.get(superTypeId)).getMemberHoldingTypeDetails();
				extendsTypes.add(superType);
			}
		}
		return extendsTypes;
	}

	public Map<JavaType, JavaType> getClientTypeMap(ClassOrInterfaceTypeDetails governorTypeDetails) {

		JavaType governorTypeName = governorTypeDetails.getName();
		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(getProjectMetadata(), governorTypeName);
		Path governorTypePath = PhysicalTypeIdentifier.getPath(governorTypeDetails.getDeclaredByMetadataId());
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetailsScanner.getMemberDetails(GwtTemplatingServiceImpl.class.getName(), governorTypeDetails).getDetails();

		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(governorTypeName, governorTypePath));
		Map<JavaType, JavaType> gwtClientTypeMap = new HashMap<JavaType, JavaType>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetail : memberHoldingTypeDetails) {

			for (MethodMetadata method : memberHoldingTypeDetail.getDeclaredMethods()) {
				if (Modifier.isPublic(method.getModifier())) {
					boolean requestType = false;
					JavaType returnType = method.getReturnType();
					if (MetadataIdentificationUtils.getMetadataClass(memberHoldingTypeDetail.getDeclaredByMetadataId()).equals(EntityMetadata.class.getName())) {
						EntityMetadata alternativeEntityMetadata = (EntityMetadata) metadataService.get(memberHoldingTypeDetail.getDeclaredByMetadataId());
							requestType = GwtUtils.isRequestMethod(alternativeEntityMetadata, method);


						if (!requestType) {
							continue;
						}

						if (!alternativeEntityMetadata.equals(entityMetadata) && !GwtUtils.isCommonType(returnType)) {

							returnType = mirrorTypeMap.get(GwtType.PROXY);
						}
					}

					boolean standardAccessor = method.getMethodName().getSymbolName().startsWith("get") || method.getMethodName().getSymbolName().startsWith("is");
					if (!standardAccessor && !requestType) {
						continue;
					}

					JavaType clientSideType = getGwtSideLeafType(returnType, getProjectMetadata(), governorTypeName, requestType);
					if (clientSideType == null) {
						continue;
					}

					gwtClientTypeMap.put(returnType, clientSideType);
					gwtClientTypeMap.put(clientSideType, clientSideType);
				}
			}
		}

		return gwtClientTypeMap;
	}

	public Map<JavaSymbolName, GwtProxyProperty> getClientSideTypeMap(List<MemberHoldingTypeDetails> memberHoldingTypeDetails, Map<JavaType, JavaType> gwtClientTypeMap) {
		Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap = new LinkedHashMap<JavaSymbolName, GwtProxyProperty>();

		for (MemberHoldingTypeDetails memberHoldingTypeDetail : memberHoldingTypeDetails) {
			for (MethodMetadata method : memberHoldingTypeDetail.getDeclaredMethods()) {
				if (Modifier.isPublic(method.getModifier()) && !method.getReturnType().equals(JavaType.VOID_PRIMITIVE) && method.getParameterTypes().size() == 0 && (method.getMethodName().getSymbolName().startsWith("get") || method.getMethodName().getSymbolName().startsWith("is") || method.getMethodName().getSymbolName().startsWith("has"))) {
					JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(method).getSymbolName()));
					if (propertyName.getSymbolName().equals("owner")) {
						logger.severe("'owner' is not allowed to be used as field name as it is currently reserved by GWT. Please rename the field 'owner' in type " + memberHoldingTypeDetail.getName().getSimpleTypeName() + ".");
						continue;
					}

					JavaType propertyType = gwtClientTypeMap.get(method.getReturnType());
					PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(propertyType, Path.SRC_MAIN_JAVA));
					if (propertyType.isCommonCollectionType()){
						ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(propertyType.getParameters().get(0), Path.SRC_MAIN_JAVA));
					}
					GwtProxyProperty gwtProxyProperty = new GwtProxyProperty(getProjectMetadata(), propertyType, ptmd, propertyName.getSymbolName(), method.getMethodName().getSymbolName());
					clientSideTypeMap.put(propertyName, gwtProxyProperty);
				}
			}
		}

		return clientSideTypeMap;
	}

	private ProjectMetadata getProjectMetadata() {
		return (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
	}
}
