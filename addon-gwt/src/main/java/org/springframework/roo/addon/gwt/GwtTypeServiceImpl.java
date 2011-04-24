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

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.AbstractIdentifiableAnnotatedJavaStructureBuilder;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.file.monitor.event.FileDetails;
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
	@Reference private FileManager fileManager;
	@Reference private GwtFileManager gwtFileManager;
	@Reference private GwtTemplateService gwtTemplateService;
	@Reference private MetadataService metadataService;
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private ProjectOperations projectOperations;

	private Set<String> warnings = new LinkedHashSet<String>();
	private Timer warningTimer = new Timer();

	/**
	 * Return the type arg for the client side method, given the domain method return type.
	 * If domain method return type is List<Integer> or Set<Integer>, returns the same.
	 * If domain method return type is List<Employee>, return List<EmployeeProxy>
	 *
	 * @param type
	 * @param projectMetadata
	 * @param governorType
	 * @return the GWT side leaf type as a JavaType
	 */
	public JavaType getGwtSideLeafType(JavaType type, ProjectMetadata projectMetadata, JavaType governorType, boolean requestType) {
		if (type.isPrimitive()) {
			if (!requestType) {
				checkPrimitive(type);
			}
			return convertPrimitiveType(type);
		}

		if (isTypeCommon(type)) {
			return type;
		}

		if (isCollectionType(type)) {
			List<JavaType> args = type.getParameters();
			if (args != null && args.size() == 1) {
				JavaType elementType = args.get(0);
				return new JavaType(type.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(getGwtSideLeafType(elementType, projectMetadata, governorType, requestType)));
			}
			return type;
		}

		PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		if (isDomainObject(type, ptmd)) {
			if (isEmbeddable(ptmd)) {
				throw new IllegalStateException("GWT does not currently support embedding objects in entities, such as '" + type.getSimpleTypeName() + "' in '" + governorType.getSimpleTypeName() + "'.");
			}
			return getDestinationJavaType(type, GwtType.PROXY, projectMetadata);
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

	public boolean isGwtModuleXmlPresent() {
		try{
			return fileManager.exists(getGwtModuleXml());
		} catch (IllegalStateException e) {
			return false;
		}
	}

	private String getGwtModuleXml() {
		String gwtModuleXml = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_JAVA, projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName().replaceAll("\\.", "/") + "/*.gwt.xml");
		Set<FileDetails> potentialXmlFiles = fileManager.findMatchingAntPath(gwtModuleXml);
		if (potentialXmlFiles.size() == 1) {
			return potentialXmlFiles.iterator().next().getCanonicalPath();
		} else if (potentialXmlFiles.size() > 1) {
			throw new IllegalStateException("Multiple gwt.xml files detected; cannot continue");
		}
		throw new IllegalStateException("No gwt.xml file detected; cannot continue");
	}

	public void buildType(GwtType type) {
		if (GwtType.LIST_PLACE_RENDERER.equals(type)) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
			watchedMethods.put(new JavaSymbolName("render"), Collections.singletonList(new JavaType(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + ".client.scaffold.place.ProxyListPlace")));
			type.setWatchedMethods(watchedMethods);
		} else {
			type.resolveMethodsToWatch(type);
		}

		type.resolveWatchedFieldNames(type);
		List<ClassOrInterfaceTypeDetails> templateTypeDetails = gwtTemplateService.getStaticTemplateTypeDetails(type);
		List<ClassOrInterfaceTypeDetails> typesToBeWritten = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (ClassOrInterfaceTypeDetails templateTypeDetail : templateTypeDetails) {
			typesToBeWritten.addAll(buildType(type, templateTypeDetail, getExtendsTypes(templateTypeDetail)));
		}
		gwtFileManager.write(typesToBeWritten, type.isOverwriteConcrete());
	}

	public List<String> getSourcePaths() {
		Assert.isTrue(isGwtModuleXmlPresent(), "GWT module's gwt.xml file not found; cannot continue");

		MutableFile mutableGwtXml;
		InputStream is = null;
		Document gwtXmlDoc;
		try {
			mutableGwtXml = fileManager.updateFile(getGwtModuleXml());
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
		MemberDetails memberDetails =  memberDetailsScanner.getMemberDetails(GwtTypeServiceImpl.class.getName(), governorTypeDetails);
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetails.getDetails();
		for (MemberHoldingTypeDetails memberHoldingTypeDetail : memberHoldingTypeDetails) {
			for (MethodMetadata method : memberHoldingTypeDetail.getDeclaredMethods()) {
				if (isPublicAccessor(method)) {
					if (isValidMethodReturnType(method, memberHoldingTypeDetail)) {
						proxyMethods.add(method);
					}
				}
			}
		}
		return proxyMethods;
	}

	public List<MethodMetadata> getRequestMethods(ClassOrInterfaceTypeDetails governorTypeDetails) {
		List<MethodMetadata> requestMethods = new LinkedList<MethodMetadata>();
		MemberDetails memberDetails =  memberDetailsScanner.getMemberDetails(GwtTypeServiceImpl.class.getName(), governorTypeDetails);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, PersistenceCustomDataKeys.PERSIST_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, PersistenceCustomDataKeys.REMOVE_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, PersistenceCustomDataKeys.COUNT_ALL_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, PersistenceCustomDataKeys.FIND_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, PersistenceCustomDataKeys.FIND_ALL_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, PersistenceCustomDataKeys.FIND_ENTRIES_METHOD);
		return requestMethods;
	}
	
	private void setRequestMethod(List<MethodMetadata> requestMethods, ClassOrInterfaceTypeDetails governorTypeDetails, MemberDetails memberDetails, Object tagKey) {
		MethodMetadata method = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, tagKey);
		if (method == null) {
			return;
		}
		JavaType gwtType = getGwtSideLeafType(method.getReturnType(), getProjectMetadata(), governorTypeDetails.getName(), true);
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(method);
		methodBuilder.setReturnType(gwtType);
		requestMethods.add(methodBuilder.build());
	}

	public boolean isValidMethodReturnType(MethodMetadata method, MemberHoldingTypeDetails memberHoldingTypeDetail) {
		JavaType returnType = method.getReturnType();
		if (isPrimitive(returnType)) {
			displayWarning("The primitive field type, " + method.getReturnType().getSimpleTypeName().toLowerCase() + " of '" + method.getMethodName().getSymbolName() + "' in type " + memberHoldingTypeDetail.getName().getSimpleTypeName() + " is not currently support by GWT and will not be added to the scaffolded application.");
			return false;
		}

		JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoUtils.getPropertyNameForJavaBeanMethod(method).getSymbolName()));
		if (!isAllowableReturnType(method)) {
			displayWarning("The field type " + method.getReturnType().getFullyQualifiedTypeName() + " of '" + method.getMethodName().getSymbolName() + "' in type " + memberHoldingTypeDetail.getName().getSimpleTypeName() + " is not currently support by GWT and will not be added to the scaffolded application.");
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
			boolean collectionTypeInSourcePath = isCollectionType(propertyType) && propertyType.getParameters().size() == 1 && propertyType.getParameters().get(0).getPackage().getFullyQualifiedPackageName().startsWith(sourcePath);
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
		return isDomainObject(type, ptmd);
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

				HashMap<JavaSymbolName, JavaType> typeMap = resolveTypes(extendsTypeDetails.getName(), concreteClass.getExtendsTypes().get(0));

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
							abstractClassBuilder.addField(convertModifier(abstractFieldBuilder));
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
								abstractClassBuilder.addMethod(convertModifier(abstractMethodBuilder));
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

	private void checkPrimitive(JavaType type) {
		if (type.isPrimitive() && !JavaType.VOID_PRIMITIVE.equals(type)) {
			String to = type.getSimpleTypeName();
			String from = to.toLowerCase();
			throw new IllegalStateException("GWT does not currently support primitive types in an entity. Please change any '" + from + "' entity property types to 'java.lang." + to + "'.");
		}
	}

	private JavaType convertPrimitiveType(JavaType type) {
		if (type != null && !JavaType.VOID_PRIMITIVE.equals(type) && type.isPrimitive()) {
			return new JavaType(type.getFullyQualifiedTypeName());
		}
		return type;
	}

	private boolean isAllowableReturnType(MethodMetadata method) {
		return isAllowableReturnType(method.getReturnType());
	}

	private boolean isAllowableReturnType(JavaType type) {
		return isCommonType(type) || isEntity(type) || isEnum(type);
	}

	private boolean isCommonType(JavaType type) {
		return isTypeCommon(type) || (isCollectionType(type) && type.getParameters().size() == 1 && isAllowableReturnType(type.getParameters().get(0)));
	}
	
	private boolean isTypeCommon(JavaType type) {
		return JavaType.BOOLEAN_OBJECT.equals(type) ||
				JavaType.CHAR_OBJECT.equals(type) ||
				JavaType.BYTE_OBJECT.equals(type) ||
				JavaType.SHORT_OBJECT.equals(type) ||
				JavaType.INT_OBJECT.equals(type) ||
				JavaType.LONG_OBJECT.equals(type) ||
				JavaType.FLOAT_OBJECT.equals(type) ||
				JavaType.DOUBLE_OBJECT.equals(type) ||
				JavaType.STRING_OBJECT.equals(type) ||
				new JavaType("java.util.Date").equals(type) ||
				new JavaType("java.math.BigDecimal").equals(type) ||
				type.isPrimitive() && !JavaType.VOID_PRIMITIVE.getFullyQualifiedTypeName().equals(type.getFullyQualifiedTypeName());
	}
	
	private boolean isEnum(JavaType type) {
		return isEnum((PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA)));
	}

	private boolean isPrimitive(JavaType type) {
		return type.isPrimitive() || (isCollectionType(type) && type.getParameters().size() == 1 && isPrimitive(type.getParameters().get(0)));
	}

	private boolean isEntity(PhysicalTypeMetadata ptmd) {
		if (ptmd == null) {
			return false;
		}
		
		AnnotationMetadata annotationMetadata = MemberFindingUtils.getDeclaredTypeAnnotation(ptmd.getMemberHoldingTypeDetails(), new JavaType("org.springframework.roo.addon.entity.RooEntity"));
		return annotationMetadata != null;
	}

	private boolean isEntity(JavaType type) {
		return isEntity((PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA)));
	}

	private boolean isCollectionType(JavaType returnType) {
		return returnType.getFullyQualifiedTypeName().equals("java.util.List") || returnType.getFullyQualifiedTypeName().equals("java.util.Set");
	}

	private boolean isEnum(PhysicalTypeMetadata ptmd) {
		return ptmd != null && ptmd.getMemberHoldingTypeDetails() != null && ptmd.getMemberHoldingTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
	}

	private boolean isEmbeddable(PhysicalTypeMetadata ptmd) {
		if (ptmd == null) {
			return false;
		}
		AnnotationMetadata annotationMetadata = MemberFindingUtils.getDeclaredTypeAnnotation(ptmd.getMemberHoldingTypeDetails(), new JavaType("javax.persistence.Embeddable"));
		return annotationMetadata != null;
	}

	private boolean isRequestFactoryCompatible(JavaType type) {
		return isCommonType(type) || isCollectionType(type);
	}

	private JavaType getDestinationJavaType(JavaType physicalType, GwtType mirrorType, ProjectMetadata projectMetadata) {
		return GwtUtils.convertGovernorTypeNameIntoKeyTypeName(physicalType, mirrorType, projectMetadata);
	}
	
	private boolean isPublicAccessor(MethodMetadata method) {
		return Modifier.isPublic(method.getModifier()) && !method.getReturnType().equals(JavaType.VOID_PRIMITIVE) && method.getParameterTypes().size() == 0 && (method.getMethodName().getSymbolName().startsWith("get"));
	}

	private boolean isDomainObject(JavaType returnType, PhysicalTypeMetadata ptmd) {
		return !isEnum(ptmd)
				&& isEntity(ptmd)
				&& !(isRequestFactoryCompatible(returnType))
				&& !isEmbeddable(ptmd);
	}

	private HashMap<JavaSymbolName, JavaType> resolveTypes(JavaType generic, JavaType typed) {
		HashMap<JavaSymbolName, JavaType> typeMap = new HashMap<JavaSymbolName, JavaType>();
		boolean typeCountMatch = generic.getParameters().size() == typed.getParameters().size();
		Assert.isTrue(typeCountMatch, "Type count must match.");

		int i = 0;
		for (JavaType genericParamType : generic.getParameters()) {
			typeMap.put(genericParamType.getArgName(), typed.getParameters().get(i));
			i++;
		}
		return typeMap;
	}
	
	private <T extends AbstractIdentifiableAnnotatedJavaStructureBuilder<? extends IdentifiableAnnotatedJavaStructure>> T convertModifier(T builder) {
		if (Modifier.isPrivate(builder.getModifier())) {
			builder.setModifier(Modifier.PROTECTED);
		}
		return builder;
	}

	private ProjectMetadata getProjectMetadata() {
		return (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
	}
}
