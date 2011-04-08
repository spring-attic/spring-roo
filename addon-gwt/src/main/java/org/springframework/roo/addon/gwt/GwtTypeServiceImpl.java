package org.springframework.roo.addon.gwt;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
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
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;

/**
 * Provides a basic implementation of {@link GwtTypeService}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
@Component
@Service
public class GwtTypeServiceImpl implements GwtTypeService {
	private static Logger logger = HandlerUtils.getLogger(GwtTypeServiceImpl.class);
	@Reference private MetadataService metadataService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private ProjectOperations projectOperations;
	@Reference private FileManager fileManager;

	private Set<String> warnings = new LinkedHashSet<String>();
	private Timer warningTimer = new Timer();

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

	public List<String> getSourcePaths() {
		String gwtXml = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName().replaceAll("\\.", "/") + "/ApplicationScaffold.gwt.xml");
		Assert.isTrue(fileManager.exists(gwtXml), "GWT module's gwt.xml file not found; cannot continue");

		MutableFile mutableGwtXml;
		InputStream is = null;
		Document gwtXmlDoc;
		try {
			mutableGwtXml = fileManager.updateFile(gwtXml);
			is = mutableGwtXml.getInputStream();
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					if (systemId.endsWith("gwt-module.dtd")) {
						return new InputSource(TemplateUtils.getTemplate(GwtMetadata.class, "templates/gwt-module.dtd"));
					}
					
					// Use the default behaviour
					return null;
				}
			});
			gwtXmlDoc = builder.parse(mutableGwtXml.getInputStream());
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


	public List<MethodMetadata> getProxyMethods(ClassOrInterfaceTypeDetails governorTypeDetails) {
		List<MethodMetadata> proxyMethods = new LinkedList<MethodMetadata>();
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetailsScanner.getMemberDetails(GwtTypeServiceImpl.class.getName(), governorTypeDetails).getDetails();
		for (MemberHoldingTypeDetails memberHoldingTypeDetail : memberHoldingTypeDetails) {
			for (MethodMetadata method : memberHoldingTypeDetail.getDeclaredMethods()) {
				if (GwtUtils.isPublicAccessor(method)) {
					if (isValidMethodReturnType(method, memberHoldingTypeDetail)) {
						proxyMethods.add(method);
					}
				}
			}
		}
		return proxyMethods;
	}

	public List<MethodMetadata> getRequestMethods(ClassOrInterfaceTypeDetails governorTypeDetails) {
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetailsScanner.getMemberDetails(GwtTypeServiceImpl.class.getName(), governorTypeDetails).getDetails();
		List<MethodMetadata> requestMethods = new LinkedList<MethodMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetail : memberHoldingTypeDetails) {
			for (MethodMetadata method : memberHoldingTypeDetail.getDeclaredMethods()) {
				if (Modifier.isPublic(method.getModifier())) {
					if (MetadataIdentificationUtils.getMetadataClass(memberHoldingTypeDetail.getDeclaredByMetadataId()).equals(EntityMetadata.class.getName())) {
						EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(memberHoldingTypeDetail.getDeclaredByMetadataId());
						if (GwtUtils.isRequestMethod(entityMetadata, method)) {
							JavaType gwtType = getGwtSideLeafType(method.getReturnType(), getProjectMetadata(), governorTypeDetails.getName(), true);
							MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(method);
							methodBuilder.setReturnType(gwtType);
							requestMethods.add(methodBuilder.build());
						}
					}
				}
			}
		}
		return requestMethods;
	}

	public boolean isValidMethodReturnType(MethodMetadata method, MemberHoldingTypeDetails memberHoldingTypeDetail) {
		JavaType returnType = method.getReturnType();
		if (isPrimitive(returnType)) {
			displayWarning("The primitive field type, " + method.getReturnType().getSimpleTypeName().toLowerCase() + " of '" + method.getMethodName().getSymbolName() + "' in type " + memberHoldingTypeDetail.getName().getSimpleTypeName() + " is not currently support by GWT and will not be added to the scaffolded application.");
			return false;
		}

		JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(method).getSymbolName()));
		if (!isAllowableReturnType(method)) {
			displayWarning("The field type, " + method.getReturnType().getFullyQualifiedTypeName() + " of '" + method.getMethodName().getSymbolName() + "' in type " + memberHoldingTypeDetail.getName().getSimpleTypeName() + " is not currently support by GWT and will not be added to the scaffolded application.");
			return false;
		}
		if (propertyName.getSymbolName().equals("owner")) {
			displayWarning("'owner' is not allowed to be used as field name as it is currently reserved by GWT. Please rename the field 'owner' in type " + memberHoldingTypeDetail.getName().getSimpleTypeName() + ".");
			return false;
		}

		return true;
	}

	public boolean isMethodReturnTypesInSourcePath(MethodMetadata method, MemberHoldingTypeDetails memberHoldingTypeDetail, List<String> sourcePaths) {
		JavaType propertyType = method.getReturnType();
		boolean inSourcePath = false;
		for (String sourcePath : sourcePaths) {
			boolean collectionTypeInSourcePath = GwtUtils.isCollectionType(propertyType) && propertyType.getParameters().size() == 1 && propertyType.getParameters().get(0).getPackage().getFullyQualifiedPackageName().startsWith(sourcePath);
			if (propertyType.getPackage().getFullyQualifiedPackageName().startsWith(sourcePath) || collectionTypeInSourcePath) {
				inSourcePath = true;
				break;
			}
		}
		if (!inSourcePath && !isCommonType(propertyType) && !JavaType.VOID_PRIMITIVE.getFullyQualifiedTypeName().equals(propertyType.getFullyQualifiedTypeName())) {
			displayWarning("The path to type " + propertyType.getFullyQualifiedTypeName() + " which is used in type " + memberHoldingTypeDetail.getName() + " by the field '" + method.getMethodName().getSymbolName() + "' needs to be added to the module's gwt.xml file in order to be used in a Proxy.");
			return false;
		}
		return true;
	}

	public boolean isDomainObject(JavaType type) {
		PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		return GwtUtils.isDomainObject(type, ptmd);
	}

	private ClassOrInterfaceTypeDetailsBuilder createAbstractBuilder(ClassOrInterfaceTypeDetailsBuilder concreteClass, List<MemberHoldingTypeDetails> extendsTypesDetails) {
		JavaType concreteType = concreteClass.getName();
		String abstractName = concreteType.getSimpleTypeName() + "_Roo_Gwt";
		abstractName = concreteType.getPackage().getFullyQualifiedPackageName() + '.' + abstractName;
		JavaType abstractType = new JavaType(abstractName);
		String abstractId = PhysicalTypeIdentifier.createIdentifier(abstractType, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(abstractId);
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
		builder.setName(abstractType);
		builder.setModifier(Modifier.ABSTRACT | Modifier.PUBLIC);
		builder.getExtendsTypes().addAll(concreteClass.getExtendsTypes());
		builder.getRegisteredImports().addAll(concreteClass.getRegisteredImports());

		for (MemberHoldingTypeDetails extendsTypeDetails : extendsTypesDetails) {
			for (ConstructorMetadata constructorMetadata : extendsTypeDetails.getDeclaredConstructors()) {
				ConstructorMetadataBuilder abstractConstructor = new ConstructorMetadataBuilder(abstractId);
				abstractConstructor.setModifier(constructorMetadata.getModifier());

				HashMap<JavaSymbolName, JavaType> typeMap = GwtUtils.resolveTypes(extendsTypeDetails.getName(), concreteClass.getExtendsTypes().get(0));

				for (AnnotatedJavaType type : constructorMetadata.getParameterTypes()) {
					JavaType newType = type.getJavaType();
					if (type.getJavaType().getParameters().size() > 0) {
						ArrayList<JavaType> paramTypes = new ArrayList<JavaType>();
						for (JavaType typeType : type.getJavaType().getParameters()) {
							JavaType typeParam = typeMap.get(new JavaSymbolName(typeType.toString()));
							if (typeParam != null) {
								paramTypes.add(typeParam);
							}
						}
						newType = new JavaType(type.getJavaType().getFullyQualifiedTypeName(), type.getJavaType().getArray(), type.getJavaType().getDataType(), type.getJavaType().getArgName(), paramTypes);
					}
					abstractConstructor.getParameterTypes().add(new AnnotatedJavaType(newType, null));
				}
				abstractConstructor.setParameterNames(constructorMetadata.getParameterNames());

				InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
				bodyBuilder.newLine().indent().append("super(");

				int i = 0;
				for (JavaSymbolName paramName : abstractConstructor.getParameterNames()) {
					bodyBuilder.append(" ").append(paramName.getSymbolName());
					if (abstractConstructor.getParameterTypes().size() > i + 1) {
						bodyBuilder.append(", ");
					}
					i++;
				}

				bodyBuilder.append(");");

				bodyBuilder.newLine().indentRemove();
				abstractConstructor.setBodyBuilder(bodyBuilder);
				builder.getDeclaredConstructors().add(abstractConstructor);
			}
		}
		return builder;
	}

	public List<ClassOrInterfaceTypeDetails> buildType(GwtType destType, ClassOrInterfaceTypeDetails templateClass, List<MemberHoldingTypeDetails> extendsTypes) {
		try {
			// A type may consist of a concrete type which depend on
			List<ClassOrInterfaceTypeDetails> types = new ArrayList<ClassOrInterfaceTypeDetails>();
			ClassOrInterfaceTypeDetailsBuilder templateClassBuilder = new ClassOrInterfaceTypeDetailsBuilder(templateClass);

			if (destType.isCreateAbstract()) {
				ClassOrInterfaceTypeDetailsBuilder abstractClassBuilder = createAbstractBuilder(templateClassBuilder, extendsTypes);

				ArrayList<FieldMetadataBuilder> fieldsToRemove = new ArrayList<FieldMetadataBuilder>();
				for (JavaSymbolName fieldName : destType.getWatchedFieldNames()) {
					for (FieldMetadataBuilder fieldBuilder : templateClassBuilder.getDeclaredFields()) {
						if (fieldBuilder.getFieldName().equals(fieldName)) {
							FieldMetadataBuilder abstractFieldBuilder = new FieldMetadataBuilder(abstractClassBuilder.getDeclaredByMetadataId(), fieldBuilder.build());
							abstractClassBuilder.addField(GwtUtils.convertModifier(abstractFieldBuilder));
							fieldsToRemove.add(fieldBuilder);
							break;
						}
					}
				}

				templateClassBuilder.getDeclaredFields().removeAll(fieldsToRemove);

				ArrayList<MethodMetadataBuilder> methodsToRemove = new ArrayList<MethodMetadataBuilder>();
				for (JavaSymbolName methodName : destType.getWatchedMethods().keySet()) {
					for (MethodMetadataBuilder methodBuilder : templateClassBuilder.getDeclaredMethods()) {
						if (methodBuilder.getMethodName().equals(methodName)) {
							if (destType.getWatchedMethods().get(methodName).containsAll(AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodBuilder.getParameterTypes()))) {
								MethodMetadataBuilder abstractMethodBuilder = new MethodMetadataBuilder(abstractClassBuilder.getDeclaredByMetadataId(), methodBuilder.build());
								abstractClassBuilder.addMethod(GwtUtils.convertModifier(abstractMethodBuilder));
								methodsToRemove.add(methodBuilder);
								break;
							}
						}
					}
				}

				templateClassBuilder.getDeclaredMethods().removeAll(methodsToRemove);

				for (JavaType innerTypeName : destType.getWatchedInnerTypes()) {
					for (ClassOrInterfaceTypeDetailsBuilder innerType : templateClassBuilder.getDeclaredInnerTypes()) {
						if (innerType.getName().getFullyQualifiedTypeName().equals(innerTypeName.getFullyQualifiedTypeName())) {
							ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(abstractClassBuilder.getDeclaredByMetadataId(), innerType.build());
							builder.setName(new JavaType(innerType.getName().getSimpleTypeName() + "_Roo_Gwt", 0, DataType.TYPE, null, innerType.getName().getParameters()));

							templateClassBuilder.getDeclaredInnerTypes().remove(innerType);
							if (innerType.getPhysicalTypeCategory().equals(PhysicalTypeCategory.INTERFACE)) {
								ClassOrInterfaceTypeDetailsBuilder innerTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(innerType.build());
								abstractClassBuilder.addInnerType(builder);
								templateClassBuilder.getDeclaredInnerTypes().remove(innerType);
								innerTypeBuilder.getDeclaredMethods().clear();
								innerTypeBuilder.getDeclaredInnerTypes().clear();
								innerTypeBuilder.getExtendsTypes().clear();
								innerTypeBuilder.getExtendsTypes().add(new JavaType(builder.getName().getSimpleTypeName(), 0, DataType.TYPE, null, Collections.singletonList(new JavaType("V", 0, DataType.VARIABLE, null, new ArrayList<JavaType>()))));
								templateClassBuilder.getDeclaredInnerTypes().add(innerTypeBuilder);
							}
							break;
						}
					}
				}

				abstractClassBuilder.setImplementsTypes(templateClass.getImplementsTypes());
				templateClassBuilder.getImplementsTypes().clear();

				templateClassBuilder.getExtendsTypes().clear();
				templateClassBuilder.getExtendsTypes().add(abstractClassBuilder.getName());

				types.add(abstractClassBuilder.build());
			}

			types.add(templateClassBuilder.build());

			return types;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
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
