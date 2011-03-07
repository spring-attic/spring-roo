package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
	@Reference private ProjectOperations projectOperations;
	@Reference private FileManager fileManager;

	private Set<String> warnings = new LinkedHashSet<String>();
	private Timer warningTimer = new Timer();

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

					if (!requestType && isPrimitive(returnType)) {
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

	private List<String> getSourcePaths() {
		String gwtXml = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName().replaceAll("\\.", "/") + "/ApplicationScaffold.gwt.xml");
		Assert.isTrue(fileManager.exists(gwtXml), "GWT module's gwt.xml file not found; cannot continue");

		MutableFile mutableGwtXml = null;
		InputStream is = null;
		Document gwtXmlDoc;
		try {
			mutableGwtXml = fileManager.updateFile(gwtXml);
			is = mutableGwtXml.getInputStream();
			gwtXmlDoc = XmlUtils.getDocumentBuilder().parse(mutableGwtXml.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		Element gwtXmlRoot = gwtXmlDoc.getDocumentElement();
		ArrayList<String> sourcePaths = new ArrayList<String>();
		List<Element> sourcePathElements = XmlUtils.findElements("/module/source", gwtXmlRoot);
		for (Element sourcePathElement : sourcePathElements) {
			String path = projectOperations.getProjectMetadata().getTopLevelPackage() + "." + sourcePathElement.getAttribute("path");
			sourcePaths.add(path);
		}

		return sourcePaths;
	}

	public Map<JavaSymbolName, GwtProxyProperty> getClientSideTypeMap(ClassOrInterfaceTypeDetails governorTypeDetails) {
		JavaType governorTypeName = governorTypeDetails.getName();
		Map<GwtType, JavaType> mirrorTypeMap = GwtUtils.getMirrorTypeMap(getProjectMetadata(), governorTypeName);
		Path governorTypePath = PhysicalTypeIdentifier.getPath(governorTypeDetails.getDeclaredByMetadataId());
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetailsScanner.getMemberDetails(GwtTemplatingServiceImpl.class.getName(), governorTypeDetails).getDetails();

		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(governorTypeName, governorTypePath));

		List<String> sourcePaths = getSourcePaths();

		Map<JavaSymbolName, GwtProxyProperty> clientSideTypeMap = new LinkedHashMap<JavaSymbolName, GwtProxyProperty>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetail : memberHoldingTypeDetails) {
			for (MethodMetadata method : memberHoldingTypeDetail.getDeclaredMethods()) {
				if (Modifier.isPublic(method.getModifier()) && !method.getReturnType().equals(JavaType.VOID_PRIMITIVE) && method.getParameterTypes().size() == 0 && (method.getMethodName().getSymbolName().startsWith("get") || method.getMethodName().getSymbolName().startsWith("is") || method.getMethodName().getSymbolName().startsWith("has"))) {

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

					if (!requestType && isPrimitive(returnType)) {
						displayWarning("The primitive field type, " + method.getReturnType().getSimpleTypeName().toLowerCase() + " of '" + method.getMethodName().getSymbolName() + "' in type " + governorTypeDetails.getName().getSimpleTypeName() + " is not currently support by GWT and will not be added to the scaffolded application.");
						continue;
					}

					JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(method).getSymbolName()));
					if (!isAllowableReturnType(method)) {
						displayWarning("The field type, " + method.getReturnType().getFullyQualifiedTypeName() + " of '" + method.getMethodName().getSymbolName() + "' in type " + governorTypeDetails.getName().getSimpleTypeName() + " is not currently support by GWT and will not be added to the scaffolded application.");
						continue;
					}
					if (propertyName.getSymbolName().equals("owner")) {
						displayWarning("'owner' is not allowed to be used as field name as it is currently reserved by GWT. Please rename the field 'owner' in type " + memberHoldingTypeDetail.getName().getSimpleTypeName() + ".");
						continue;
					}

					JavaType propertyType = getGwtSideLeafType(returnType, getProjectMetadata(), governorTypeName, requestType);
					if (propertyType == null) {
						continue;
					}

					boolean inSourcePath = false;
					for (String sourcePath : sourcePaths) {
						boolean collectionTypeInSourcePath = GwtUtils.isCollectionType(propertyType) && propertyType.getParameters().size() == 1 && propertyType.getParameters().get(0).getPackage().getFullyQualifiedPackageName().startsWith(sourcePath);
						if (propertyType.getPackage().getFullyQualifiedPackageName().startsWith(sourcePath) || collectionTypeInSourcePath) {
							inSourcePath = true;
							break;
						}
					}
					if (!inSourcePath && !isCommonType(propertyType) && !JavaType.VOID_PRIMITIVE.getFullyQualifiedTypeName().equals(propertyType.getFullyQualifiedTypeName())) {
						displayWarning("The path to type " + returnType.getFullyQualifiedTypeName() + " which is used in type " + governorTypeDetails.getName() + " by the field '" + method.getMethodName().getSymbolName() + "' needs to be added to the module's gwt.xml file in order to be used in a Proxy.");
						continue;
					}

					PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(propertyType, Path.SRC_MAIN_JAVA));

					if (propertyType.isCommonCollectionType()) {
						ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(propertyType.getParameters().get(0), Path.SRC_MAIN_JAVA));
					}

					GwtProxyProperty gwtProxyProperty = new GwtProxyProperty(getProjectMetadata(), propertyType, ptmd, propertyName.getSymbolName(), method.getMethodName().getSymbolName());
					clientSideTypeMap.put(propertyName, gwtProxyProperty);
				}
			}
		}
		return clientSideTypeMap;
	}

	private void displayWarning(String warning) {
		if (!warnings.contains(warning)) {
			warnings.add(warning);
			logger.severe(warning);
			warningTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					warnings.clear();
				}
			}, 15000);
		}
	}

	private boolean isAllowableReturnType(MethodMetadata method) {
		return isAllowableReturnType(method.getReturnType());
	}

	private boolean isAllowableReturnType(JavaType type) {
		return isCommonType(type) || isEntity(type) || isEnum(type);
	}

	private boolean isCommonType(JavaType type) {
		return GwtUtils.isCommonType(type) || (GwtUtils.isCollectionType(type) && type.getParameters().size() == 1 && isAllowableReturnType(type.getParameters().get(0)));
	}

	private boolean isPrimitive(JavaType type) {
		return type.isPrimitive() || (GwtUtils.isCollectionType(type) && type.getParameters().size() == 1 && isPrimitive(type.getParameters().get(0)));
	}

	private boolean isEntity(JavaType type) {
		PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		if (ptmd == null) {
			return false;
		}
		AnnotationMetadata annotationMetadata = MemberFindingUtils.getDeclaredTypeAnnotation(ptmd.getMemberHoldingTypeDetails(), new JavaType(RooEntity.class.getName()));
		return annotationMetadata != null;
	}

	private boolean isEnum(JavaType type) {
		return GwtUtils.isEnum((PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA)));
	}

	private ProjectMetadata getProjectMetadata() {
		return (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
	}
}
