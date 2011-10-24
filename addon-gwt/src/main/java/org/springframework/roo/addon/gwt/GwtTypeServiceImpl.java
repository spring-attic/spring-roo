package org.springframework.roo.addon.gwt;

import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.gwt.scaffold.GwtScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
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
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.IOUtils;
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

	// Constants
	private static final Logger logger = HandlerUtils.getLogger(GwtTypeServiceImpl.class);

	// Fields
	@Reference protected FileManager fileManager;
	@Reference protected GwtFileManager gwtFileManager;
	@Reference protected MetadataService metadataService;
	@Reference protected MemberDetailsScanner memberDetailsScanner;
	@Reference protected PersistenceMemberLocator persistenceMemberLocator;
	@Reference protected ProjectOperations projectOperations;
	@Reference protected TypeLocationService typeLocationService;

	private final Set<String> warnings = new LinkedHashSet<String>();
	private final Timer warningTimer = new Timer();

	/**
	 * Return the type arg for the client side method, given the domain method return type. If domain method return type is List<Integer> or Set<Integer>, returns the same. If domain method return
	 * type is List<Employee>, return List<EmployeeProxy>
	 *
	 * @param returnType
	 * @param projectMetadata
	 * @param governorType
	 * @return the GWT side leaf type as a JavaType
	 */

	public JavaType getGwtSideLeafType(final JavaType returnType, final JavaType governorType, final boolean requestType, final boolean convertPrimitive) {
		if (returnType.isPrimitive() && convertPrimitive) {
			if (!requestType) {
				checkPrimitive(returnType);
			}
			return GwtUtils.convertPrimitiveType(returnType, requestType);
		}

		if (isTypeCommon(returnType)) {
			return returnType;
		}

		if (isCollectionType(returnType)) {
			List<JavaType> args = returnType.getParameters();
			if (args != null && args.size() == 1) {
				JavaType elementType = args.get(0);
				JavaType convertedJavaType = getGwtSideLeafType(elementType, governorType, requestType, convertPrimitive);
				if (convertedJavaType == null) {
					return null;
				}
				return new JavaType(returnType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(convertedJavaType));
			}
			return returnType;
		}

		ClassOrInterfaceTypeDetails ptmd = typeLocationService.getTypeDetails(returnType);
		if (isDomainObject(returnType, ptmd)) {
			if (isEmbeddable(ptmd)) {
				throw new IllegalStateException("GWT does not currently support embedding objects in entities, such as '" + returnType.getSimpleTypeName() + "' in '" + governorType.getSimpleTypeName() + "'.");
			}
			ClassOrInterfaceTypeDetails typeDetails = typeLocationService.getTypeDetails(returnType);
			if (typeDetails == null) {
				return null;
			}
			ClassOrInterfaceTypeDetails proxy = lookupProxyFromEntity(typeDetails);
			if (proxy == null) {
				return null;
			}
			return proxy.getName();
		}
		return returnType;
	}

	public ClassOrInterfaceTypeDetails lookupRequestFromProxy(final ClassOrInterfaceTypeDetails proxy) {
		AnnotationMetadata annotation = GwtUtils.getFirstAnnotation(proxy, RooJavaType.ROO_GWT_PROXY);
		Assert.notNull(annotation, "Proxy '" + proxy.getName() + "' isn't annotated with '" + RooJavaType.ROO_GWT_PROXY + "'");
		AnnotationAttributeValue<?> attributeValue = annotation.getAttribute("value");
		JavaType serviceNameType = new JavaType(GwtUtils.getStringValue(attributeValue));
		return lookupRequestFromEntity(typeLocationService.getTypeDetails(serviceNameType));
	}

	public ClassOrInterfaceTypeDetails lookupProxyFromRequest(final ClassOrInterfaceTypeDetails request) {
		AnnotationMetadata annotation = GwtUtils.getFirstAnnotation(request, RooJavaType.ROO_GWT_REQUEST);
		Assert.notNull(annotation, "Request '" + request.getName() + "' isn't annotated with '" + RooJavaType.ROO_GWT_REQUEST + "'");
		AnnotationAttributeValue<?> attributeValue = annotation.getAttribute("value");
		JavaType proxyType = new JavaType(GwtUtils.getStringValue(attributeValue));
		return lookupProxyFromEntity(typeLocationService.getTypeDetails(proxyType));
	}

	public ClassOrInterfaceTypeDetails lookupEntityFromProxy(final ClassOrInterfaceTypeDetails proxy) {
		Assert.notNull(proxy, "Proxy is required");
		return lookupTargetFromX(proxy, RooJavaType.ROO_GWT_PROXY);
	}

	public ClassOrInterfaceTypeDetails lookupEntityFromRequest(final ClassOrInterfaceTypeDetails request) {
		Assert.notNull(request, "Request is required");
		return lookupTargetFromX(request, RooJavaType.ROO_GWT_REQUEST);
	}

	public ClassOrInterfaceTypeDetails lookupEntityFromLocator(final ClassOrInterfaceTypeDetails locator) {
		Assert.notNull(locator, "Locator is required");
		return lookupTargetFromX(locator, RooJavaType.ROO_GWT_LOCATOR);
	}

	public ClassOrInterfaceTypeDetails lookupTargetServiceFromRequest(final ClassOrInterfaceTypeDetails request) {
		Assert.notNull(request, "Request is required");
		return lookupTargetFromX(request, GwtUtils.REQUEST_ANNOTATIONS);
	}

	public ClassOrInterfaceTypeDetails lookupTargetFromX(final ClassOrInterfaceTypeDetails typeDetails, final JavaType... annotations) {
		AnnotationMetadata annotation = GwtUtils.getFirstAnnotation(typeDetails, annotations);
		Assert.notNull(annotation, "Type '" + typeDetails.getName() + "' isn't annotated with '" + StringUtils.collectionToCommaDelimitedString(Arrays.asList(annotations)) + "'");
		AnnotationAttributeValue<?> attributeValue = annotation.getAttribute("value");
		JavaType serviceNameType = new JavaType(GwtUtils.getStringValue(attributeValue));
		return typeLocationService.getTypeDetails(serviceNameType);
	}

	public ClassOrInterfaceTypeDetails lookupRequestFromEntity(final ClassOrInterfaceTypeDetails entity) {
		return lookupXFromEntity(entity, RooJavaType.ROO_GWT_REQUEST);
	}

	public ClassOrInterfaceTypeDetails lookupProxyFromEntity(final ClassOrInterfaceTypeDetails entity) {
		return lookupXFromEntity(entity, RooJavaType.ROO_GWT_PROXY);
	}

	public ClassOrInterfaceTypeDetails lookupXFromEntity(final ClassOrInterfaceTypeDetails entity, final JavaType... annotations) {
		Set<ClassOrInterfaceTypeDetails> cids = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(annotations);
		for (ClassOrInterfaceTypeDetails cid : cids) {
			AnnotationMetadata annotationMetadata = GwtUtils.getFirstAnnotation(cid, annotations);
			if (annotationMetadata != null) {
				AnnotationAttributeValue<?> attributeValue = annotationMetadata.getAttribute("value");
				String value = GwtUtils.getStringValue(attributeValue);
				if (entity.getName().getFullyQualifiedTypeName().equals(value)) {
					return cid;
				}
			}
		}
		return null;
	}

	public List<MemberHoldingTypeDetails> getExtendsTypes(final ClassOrInterfaceTypeDetails childType) {
		List<MemberHoldingTypeDetails> extendsTypes = new ArrayList<MemberHoldingTypeDetails>();
		if (childType != null) {
			for (JavaType javaType : childType.getExtendsTypes()) {
				String superTypeId = typeLocationService.getPhysicalTypeIdentifier(javaType);
				if (superTypeId == null || metadataService.get(superTypeId) == null) {
					continue;
				}
				MemberHoldingTypeDetails superType = ((PhysicalTypeMetadata) metadataService.get(superTypeId)).getMemberHoldingTypeDetails();
				extendsTypes.add(superType);
			}
		}
		return extendsTypes;
	}

	private Set<String> getGwtModuleXml(final String moduleName) {

		String gwtModuleXml = projectOperations.getPathResolver().getFocusedRoot(Path.SRC_MAIN_JAVA) + projectOperations.getTopLevelPackage(moduleName).getFullyQualifiedPackageName().replace('.', File.separatorChar) + File.separator + "*.gwt.xml";
		Set<String> paths = new LinkedHashSet<String>();
		for (FileDetails fileDetails : fileManager.findMatchingAntPath(gwtModuleXml)) {
			paths.add(fileDetails.getCanonicalPath());
		}
		return paths;
	}

	public void buildType(final GwtType type, final List<ClassOrInterfaceTypeDetails> templateTypeDetails, final String moduleName) {
		if (GwtType.LIST_PLACE_RENDERER.equals(type)) {
			HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
			watchedMethods.put(new JavaSymbolName("render"), Collections.singletonList(new JavaType(projectOperations.getTopLevelPackage(moduleName).getFullyQualifiedPackageName() + ".client.scaffold.place.ProxyListPlace")));
			type.setWatchedMethods(watchedMethods);
		} else {
			type.resolveMethodsToWatch(type);
		}

		type.resolveWatchedFieldNames(type);
		List<ClassOrInterfaceTypeDetails> typesToBeWritten = new ArrayList<ClassOrInterfaceTypeDetails>();
		for (ClassOrInterfaceTypeDetails templateTypeDetail : templateTypeDetails) {
			typesToBeWritten.addAll(buildType(type, templateTypeDetail, getExtendsTypes(templateTypeDetail)));
		}
		gwtFileManager.write(typesToBeWritten, type.isOverwriteConcrete());
	}

	public Set<String> getSourcePaths(final String moduleName) {
		Set<String> sourcePaths = new HashSet<String>();
		Set<String> gwtModuleXml = getGwtModuleXml(moduleName);
		Assert.isTrue(!gwtModuleXml.isEmpty(), "GWT module XML file(s) not found");
		for (String gwtModuleCanonicalPath : gwtModuleXml) {
			sourcePaths.addAll(getSourcePaths(gwtModuleCanonicalPath, moduleName));
		}
		return sourcePaths;
	}

	public Set<String> getSourcePaths(final String gwtModuleCanonicalPath, final String moduleName) {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		builder.setEntityResolver(new EntityResolver() {
			public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
				if (systemId.endsWith("gwt-module.dtd")) {
					return new InputSource(TemplateUtils.getTemplate(GwtScaffoldMetadata.class, "templates/gwt-module.dtd"));
				}

				// Use the default behaviour
				return null;
			}
		});

		Document gwtXmlDoc;
		InputStream inputStream = null;
		try {
			inputStream = fileManager.getInputStream(gwtModuleCanonicalPath);
			gwtXmlDoc = builder.parse(inputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		Element gwtXmlRoot = gwtXmlDoc.getDocumentElement();
		Set<String> sourcePaths = new HashSet<String>();
		List<Element> sourcePathElements = XmlUtils.findElements("/module/source", gwtXmlRoot);
		for (Element sourcePathElement : sourcePathElements) {
			String path = projectOperations.getTopLevelPackage(moduleName) + "." + sourcePathElement.getAttribute("path");
			sourcePaths.add(path);
		}

		return sourcePaths;
	}

	public Map<JavaSymbolName, MethodMetadata> getProxyMethods(final ClassOrInterfaceTypeDetails governorTypeDetails) {
		Map<JavaSymbolName, MethodMetadata> proxyMethods = new LinkedHashMap<JavaSymbolName, MethodMetadata>();
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(GwtTypeServiceImpl.class.getName(), governorTypeDetails);
		List<MemberHoldingTypeDetails> memberHoldingTypeDetails = memberDetails.getDetails();
		for (MemberHoldingTypeDetails memberHoldingTypeDetail : memberHoldingTypeDetails) {
			for (MethodMetadata method : memberDetails.getMethods()) {
				if (isPublicAccessor(method)) {
					if (isValidMethodReturnType(method, memberHoldingTypeDetail)) {
						proxyMethods.remove(method.getMethodName());
						proxyMethods.put(method.getMethodName(), method);
					}
				}
			}
		}
		return proxyMethods;
	}

	public List<MethodMetadata> getRequestMethods(final ClassOrInterfaceTypeDetails governorTypeDetails) {
		List<MethodMetadata> requestMethods = new ArrayList<MethodMetadata>();
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(GwtTypeServiceImpl.class.getName(), governorTypeDetails);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, CustomDataKeys.PERSIST_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, CustomDataKeys.REMOVE_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, CustomDataKeys.COUNT_ALL_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, CustomDataKeys.FIND_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, CustomDataKeys.FIND_ALL_METHOD);
		setRequestMethod(requestMethods, governorTypeDetails, memberDetails, CustomDataKeys.FIND_ENTRIES_METHOD);
		return requestMethods;
	}

	private void setRequestMethod(final List<MethodMetadata> requestMethods, final ClassOrInterfaceTypeDetails governorTypeDetails, final MemberDetails memberDetails, final Object tagKey) {
		MethodMetadata method = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, tagKey);
		if (method == null) {
			return;
		}
		JavaType gwtType = getGwtSideLeafType(method.getReturnType(), governorTypeDetails.getName(), true, true);
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(method);
		methodBuilder.setReturnType(gwtType);
		requestMethods.add(methodBuilder.build());
	}

	public boolean isValidMethodReturnType(final MethodMetadata method, final MemberHoldingTypeDetails memberHoldingTypeDetail) {
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

	public boolean isMethodReturnTypesInSourcePath(final MethodMetadata method, final MemberHoldingTypeDetails memberHoldingTypeDetail, final Set<String> sourcePaths) {
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

	public boolean isDomainObject(final JavaType type) {
		ClassOrInterfaceTypeDetails ptmd = typeLocationService.getTypeDetails(type);
		return isDomainObject(type, ptmd);
	}

	private ClassOrInterfaceTypeDetailsBuilder createAbstractBuilder(final ClassOrInterfaceTypeDetailsBuilder concreteClass, final List<MemberHoldingTypeDetails> extendsTypesDetails) {
		JavaType concreteType = concreteClass.getName();
		String abstractName = concreteType.getSimpleTypeName() + "_Roo_Gwt";
		abstractName = concreteType.getPackage().getFullyQualifiedPackageName() + '.' + abstractName;
		JavaType abstractType = new JavaType(abstractName);
		String abstractId = PhysicalTypeIdentifier.createIdentifier(abstractType, projectOperations.getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA));
		ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(abstractId);
		builder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
		builder.setName(abstractType);
		builder.setModifier(Modifier.ABSTRACT | Modifier.PUBLIC);
		builder.getExtendsTypes().addAll(concreteClass.getExtendsTypes());
		builder.add(concreteClass.getRegisteredImports());

		for (MemberHoldingTypeDetails extendsTypeDetails : extendsTypesDetails) {
			for (ConstructorMetadata constructorMetadata : extendsTypeDetails.getDeclaredConstructors()) {
				ConstructorMetadataBuilder abstractConstructor = new ConstructorMetadataBuilder(abstractId);
				abstractConstructor.setModifier(constructorMetadata.getModifier());

				Map<JavaSymbolName, JavaType> typeMap = resolveTypes(extendsTypeDetails.getName(), concreteClass.getExtendsTypes().get(0));

				for (AnnotatedJavaType type : constructorMetadata.getParameterTypes()) {
					JavaType newType = type.getJavaType();
					if (type.getJavaType().getParameters().size() > 0) {
						ArrayList<JavaType> parameterTypes = new ArrayList<JavaType>();
						for (JavaType typeType : type.getJavaType().getParameters()) {
							JavaType typeParam = typeMap.get(new JavaSymbolName(typeType.toString()));
							if (typeParam != null) {
								parameterTypes.add(typeParam);
							}
						}
						newType = new JavaType(type.getJavaType().getFullyQualifiedTypeName(), type.getJavaType().getArray(), type.getJavaType().getDataType(), type.getJavaType().getArgName(), parameterTypes);
					}
					abstractConstructor.getParameterTypes().add(new AnnotatedJavaType(newType));
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

	public List<ClassOrInterfaceTypeDetails> buildType(final GwtType destType, final ClassOrInterfaceTypeDetails templateClass, final List<MemberHoldingTypeDetails> extendsTypes) {
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

				final List<MethodMetadataBuilder> methodsToRemove = new ArrayList<MethodMetadataBuilder>();
				for (JavaSymbolName methodName : destType.getWatchedMethods().keySet()) {
					for (MethodMetadataBuilder methodBuilder : templateClassBuilder.getDeclaredMethods()) {
						List<JavaType> params = new ArrayList<JavaType>();
						for (AnnotatedJavaType param : methodBuilder.getParameterTypes()) {
							params.add(new JavaType(param.getJavaType().getFullyQualifiedTypeName()));
						}
						if (methodBuilder.getMethodName().equals(methodName)) {
							if (destType.getWatchedMethods().get(methodName).containsAll(params)) {
								MethodMetadataBuilder abstractMethodBuilder = new MethodMetadataBuilder(abstractClassBuilder.getDeclaredByMetadataId(), methodBuilder.build());
								abstractClassBuilder.addMethod(convertModifier(abstractMethodBuilder));
								methodsToRemove.add(methodBuilder);
								break;
							}
						}
					}
				}

				templateClassBuilder.removeAll(methodsToRemove);

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
								innerTypeBuilder.clearDeclaredMethods();
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

	private void displayWarning(final String warning) {
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

	private void checkPrimitive(final JavaType type) {
		if (type.isPrimitive() && !JavaType.VOID_PRIMITIVE.equals(type)) {
			String to = type.getSimpleTypeName();
			String from = to.toLowerCase();
			throw new IllegalStateException("GWT does not currently support primitive types in an entity. Please change any '" + from + "' entity property types to 'java.lang." + to + "'.");
		}
	}

	private boolean isAllowableReturnType(final MethodMetadata method) {
		return isAllowableReturnType(method.getReturnType());
	}

	private boolean isAllowableReturnType(final JavaType type) {
		return isCommonType(type) || isEntity(type) || isEnum(type);
	}

	private boolean isCommonType(final JavaType type) {
		return isTypeCommon(type) || (isCollectionType(type) && type.getParameters().size() == 1 && isAllowableReturnType(type.getParameters().get(0)));
	}

	private boolean isTypeCommon(final JavaType type) {
		return JavaType.BOOLEAN_OBJECT.equals(type)
			|| JavaType.CHAR_OBJECT.equals(type)
			|| JavaType.BYTE_OBJECT.equals(type)
			|| JavaType.SHORT_OBJECT.equals(type)
			|| JavaType.INT_OBJECT.equals(type)
			|| LONG_OBJECT.equals(type)
			|| JavaType.FLOAT_OBJECT.equals(type)
			|| JavaType.DOUBLE_OBJECT.equals(type)
			|| JavaType.STRING.equals(type)
			|| DATE.equals(type)
			|| BIG_DECIMAL.equals(type)
			|| type.isPrimitive()
			&& !JavaType.VOID_PRIMITIVE.getFullyQualifiedTypeName().equals(type.getFullyQualifiedTypeName());
	}

	private boolean isEnum(final JavaType type) {
		return isEnum(typeLocationService.getTypeDetails(type));
	}

	private boolean isPrimitive(final JavaType type) {
		return type.isPrimitive() || (isCollectionType(type) && type.getParameters().size() == 1 && isPrimitive(type.getParameters().get(0)));
	}

	private boolean isEntity(final JavaType type) {
		return persistenceMemberLocator.getIdentifierFields(type).size() == 1;
	}

	private boolean isCollectionType(final JavaType returnType) {
		return returnType.getFullyQualifiedTypeName().equals(LIST.getFullyQualifiedTypeName()) || returnType.getFullyQualifiedTypeName().equals(SET.getFullyQualifiedTypeName());
	}

	private boolean isEnum(final ClassOrInterfaceTypeDetails ptmd) {
		return ptmd != null && ptmd.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
	}

	private boolean isEmbeddable(final ClassOrInterfaceTypeDetails ptmd) {
		if (ptmd == null) {
			return false;
		}
		AnnotationMetadata annotationMetadata = ptmd.getAnnotation(EMBEDDABLE);
		return annotationMetadata != null;
	}

	private boolean isRequestFactoryCompatible(final JavaType type) {
		return isCommonType(type) || isCollectionType(type);
	}

	private boolean isPublicAccessor(final MethodMetadata method) {
		return Modifier.isPublic(method.getModifier()) && !method.getReturnType().equals(JavaType.VOID_PRIMITIVE) && method.getParameterTypes().isEmpty() && (method.getMethodName().getSymbolName().startsWith("get"));
	}

	private boolean isDomainObject(final JavaType returnType, final ClassOrInterfaceTypeDetails ptmd) {
		return !isEnum(ptmd) && isEntity(returnType) && !(isRequestFactoryCompatible(returnType)) && !isEmbeddable(ptmd);
	}

	private Map<JavaSymbolName, JavaType> resolveTypes(final JavaType generic, final JavaType typed) {
		Map<JavaSymbolName, JavaType> typeMap = new LinkedHashMap<JavaSymbolName, JavaType>();
		boolean typeCountMatch = generic.getParameters().size() == typed.getParameters().size();
		Assert.isTrue(typeCountMatch, "Type count must match.");

		int i = 0;
		for (JavaType genericParamType : generic.getParameters()) {
			typeMap.put(genericParamType.getArgName(), typed.getParameters().get(i));
			i++;
		}
		return typeMap;
	}

	private <T extends AbstractIdentifiableAnnotatedJavaStructureBuilder<? extends IdentifiableAnnotatedJavaStructure>> T convertModifier(final T builder) {
		if (Modifier.isPrivate(builder.getModifier())) {
			builder.setModifier(Modifier.PROTECTED);
		}
		return builder;
	}
}
