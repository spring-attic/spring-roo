package org.springframework.roo.addon.web.mvc.jsp;

import java.beans.Introspector;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlRoundTripUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helper class which generates the contents of the various jsp documents
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
public class JspViewManager {
	
	private List<FieldMetadata> fields; 
	private BeanInfoMetadata beanInfoMetadata; 
	private EntityMetadata entityMetadata;
	private MetadataService metadataService;
	private FinderMetadata finderMetadata;
	private WebScaffoldAnnotationValues webScaffoldAnnotationValues;
	private final String entityName;
	private final String controllerPath;
	private Map<JavaType, String> pluralCache;
	
	public JspViewManager(MetadataService metadataService, List<FieldMetadata> fields, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FinderMetadata finderMetadata, WebScaffoldAnnotationValues webScaffoldAnnotationValues) {
		Assert.notNull(fields, "List of fields required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.notNull(finderMetadata, "Finder metadata required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(webScaffoldAnnotationValues, "Web scaffold annotation values required");
		this.fields = fields;
		
		this.pluralCache = new HashMap<JavaType, String>();
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.metadataService = metadataService;
		this.finderMetadata = finderMetadata;
		this.webScaffoldAnnotationValues = webScaffoldAnnotationValues;

		entityName = StringUtils.uncapitalize(beanInfoMetadata.getJavaBean().getSimpleTypeName());
		
		Assert.notNull(webScaffoldAnnotationValues.getPath(), "Path is not specified in the @RooWebScaffold annotation for '" + webScaffoldAnnotationValues.getGovernorTypeDetails().getName() + "'");
		
		if (webScaffoldAnnotationValues.getPath().startsWith("/")) {
			controllerPath = webScaffoldAnnotationValues.getPath();
		} else {
			controllerPath = "/" + webScaffoldAnnotationValues.getPath();
		}	
	}
	
	public Document getListDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();
		
		//add document namespaces
		Element div = new XmlElementBuilder("div", document)
								.addAttribute("xmlns:page", "urn:jsptagdir:/WEB-INF/tags/form")
								.addAttribute("xmlns:table", "urn:jsptagdir:/WEB-INF/tags/form/fields")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build();
		document.appendChild(div);
		
		Element fieldTable = new XmlElementBuilder("table:table", document)
								.addAttribute("id", "l:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("data", "${" + getPlural(beanInfoMetadata.getJavaBean()).toLowerCase() + "}")
								.addAttribute("path", controllerPath)
							.build();
		
		if (!webScaffoldAnnotationValues.isUpdate()) {
			fieldTable.setAttribute("update", "false");
		}
		if (!webScaffoldAnnotationValues.isDelete()) {
			fieldTable.setAttribute("delete", "false");
		}
		fieldTable.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(fieldTable));
		
		int fieldCounter = 0;
		for (FieldMetadata field : fields) {
			if(++fieldCounter < 7) {
				Element columnElement = new XmlElementBuilder("table:column", document)
											.addAttribute("id", "c:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + field.getFieldName().getSymbolName())
											.addAttribute("property", Introspector.decapitalize(StringUtils.capitalize(field.getFieldName().getSymbolName())))
										.build();
				columnElement.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(columnElement));
				fieldTable.appendChild(columnElement);
			}
		}
		
		//create page:list element
		Element pageList = new XmlElementBuilder("page:list", document)
								.addAttribute("id", "pl:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("items", "${" + getPlural(beanInfoMetadata.getJavaBean()).toLowerCase() + "}")
								.addChild(fieldTable)
							.build();
		
		pageList.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(pageList));

		div.appendChild(pageList);
		
		return document;
	}
	
	public Document getShowDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();
			
		//add document namespaces
		Element div = (Element) document.appendChild(new XmlElementBuilder("div", document)
								.addAttribute("xmlns:page", "urn:jsptagdir:/WEB-INF/tags/form")
								.addAttribute("xmlns:field", "urn:jsptagdir:/WEB-INF/tags/form/fields")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build());
		
		Element pageShow = new XmlElementBuilder("page:show", document)
								.addAttribute("id", "ps:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("object", "${" + entityName.toLowerCase() + "}")
							.build();
		pageShow.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(pageShow));

		//add field:display elements for each field
		for (FieldMetadata field : fields) {
			//ignoring java.util.Map field types (see ROO-194)
			if (field.getFieldType().equals(new JavaType(Map.class.getName()))) {
				continue;
			}
			String fieldName = Introspector.decapitalize(StringUtils.capitalize(field.getFieldName().getSymbolName()));
			Element fieldDisplay = new XmlElementBuilder("field:display", document)
								.addAttribute("id", "s:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + field.getFieldName().getSymbolName())
								.addAttribute("object", "${" + entityName.toLowerCase() + "}")
								.addAttribute("field", fieldName)
							.build();
			if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				fieldDisplay.setAttribute("date", "true");
				fieldDisplay.setAttribute("dateTimePattern", "${" + entityName + "_" + fieldName.toLowerCase() + "_date_format}");
			} else if (field.getFieldType().equals(new JavaType(Calendar.class.getName()))) {
				fieldDisplay.setAttribute("calendar", "true");
				fieldDisplay.setAttribute("dateTimePattern", "${" + entityName + "_" + fieldName.toLowerCase() + "_date_format}");
			}
			fieldDisplay.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(fieldDisplay));

			pageShow.appendChild(fieldDisplay);
		}
		div.appendChild(pageShow);
		
		return document;
	}
	
	public Document getCreateDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();
			
		//add document namespaces
		Element div = (Element) document.appendChild(new XmlElementBuilder("div", document)
								.addAttribute("xmlns:form", "urn:jsptagdir:/WEB-INF/tags/form")
								.addAttribute("xmlns:field", "urn:jsptagdir:/WEB-INF/tags/form/fields")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("xmlns:c", "http://java.sun.com/jsp/jstl/core")
								.addAttribute("xmlns:spring", "http://www.springframework.org/tags")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build());

		//add form create element
		Element formCreate = new XmlElementBuilder("form:create", document)
						.addAttribute("id", "fc:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
						.addAttribute("modelAttribute", entityName)
						.addAttribute("path", controllerPath)
						.addAttribute("render", "${empty dependencies}")
					.build();
		
		if (!controllerPath.toLowerCase().equals(beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase())) {
			formCreate.setAttribute("path", controllerPath);
		}
		
		createFieldsForCreateAndUpdate(document, formCreate, "create");
		formCreate.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(formCreate));
		
		Element fieldReference = new XmlElementBuilder("field:reference", document)
										.addAttribute("field", "${dependency[0]}")
										.addAttribute("id", "s:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + ".${dependency[0]}")
										.addAttribute("path", "/${dependency[1]}").build();
		fieldReference.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(fieldReference));
		
		Element message = new XmlElementBuilder("spring:message", document).addAttribute("id", "ms:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName()).addAttribute("code", "entity.dependency.required").build();
		message.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(message));
		
		Element p = new XmlElementBuilder("p", document).addAttribute("id", "p:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName()).addChild(fieldReference).build();
		p.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(p));
		
		Element forEach = new XmlElementBuilder("c:forEach", document).addAttribute("id", "fe:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName()).addAttribute("var", "dependency").addAttribute("items", "${dependencies}")
								.addChild(p)
							.build();
		forEach.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(forEach));
		
		Element formPage = new XmlElementBuilder("form:page", document)
									.addAttribute("id", "fs:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
									.addAttribute("title", beanInfoMetadata.getJavaBean().getSimpleTypeName())
									.addAttribute("render", "${not empty dependencies}")
									.addChild(message)
									.addChild(forEach)
								.build();
		formPage.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(formPage));

		div.appendChild(formCreate);
		div.appendChild(formPage);

		return document;
	}
	
	public Document getUpdateDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();
			
		//add document namespaces
		Element div = (Element) document.appendChild(new XmlElementBuilder("div", document)
								.addAttribute("xmlns:form", "urn:jsptagdir:/WEB-INF/tags/form")
								.addAttribute("xmlns:field", "urn:jsptagdir:/WEB-INF/tags/form/fields")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build());

		//add form update element
		Element formUpdate = new XmlElementBuilder("form:update", document)
						.addAttribute("id", "fu:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
						.addAttribute("modelAttribute", entityName)
					.build();	
		
		if (!controllerPath.toLowerCase().equals(beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase())) {
			formUpdate.setAttribute("path", controllerPath);
		}
		if (!"id".equals(entityMetadata.getIdentifierField().getFieldName().getSymbolName())) {
			formUpdate.setAttribute("idField", entityMetadata.getIdentifierField().getFieldName().getSymbolName());
		}
		if (null == entityMetadata.getVersionField()) {
			formUpdate.setAttribute("versionField", "none");
		} else if (!"version".equals(entityMetadata.getVersionField().getFieldName().getSymbolName())) {
			formUpdate.setAttribute("versionField", entityMetadata.getVersionField().getFieldName().getSymbolName());
		}
		
		createFieldsForCreateAndUpdate(document, formUpdate, "update");
		
		formUpdate.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(formUpdate));
		
		div.appendChild(formUpdate);
		
		return document;
	}
	
	public Document getFinderDocument(String finderName) {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();
			
		//add document namespaces
		Element div = (Element) document.appendChild(new XmlElementBuilder("div", document)
								.addAttribute("xmlns:form", "urn:jsptagdir:/WEB-INF/tags/form")
								.addAttribute("xmlns:field", "urn:jsptagdir:/WEB-INF/tags/form/fields")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build());
		
		Element formFind = new XmlElementBuilder("form:find", document)
								.addAttribute("id", "ff:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("path", controllerPath)
								.addAttribute("finderName", finderName.replace("find" + entityMetadata.getPlural(), ""))
							.build();
		formFind.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(formFind));
		
		div.appendChild(formFind);
		
		MethodMetadata methodMetadata = finderMetadata.getDynamicFinderMethod(finderName, beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase());
		
		List<JavaType> types = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());
		List<JavaSymbolName> paramNames = methodMetadata.getParameterNames();
		
		for (int i = 0; i < types.size(); i++) {
			JavaType type = types.get(i);
			JavaSymbolName paramName = paramNames.get(i);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(paramName);
			if (field == null) {
				//it may be that the field has an min or max prepended
				field = beanInfoMetadata.getFieldForPropertyName(new JavaSymbolName(Introspector.decapitalize(paramName.getSymbolName().substring(3))));
			}
			//ignoring java.util.Map field types (see ROO-194)
			if (field.getFieldType().equals(new JavaType(Map.class.getName()))) {
				continue;
			}
			Assert.notNull(field, "could not find field '" + paramName + "' in '" + type.getFullyQualifiedTypeName() + "'");
			Element fieldElement = null;
			
			if (type.isCommonCollectionType() && isSpecialType(type.getParameters().get(0))) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type.getParameters().get(0), Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					fieldElement = new XmlElementBuilder("field:select", document)
										.addAttribute("items", "${" + getPlural(type.getParameters().get(0)).toLowerCase() + "}")
										.addAttribute("itemValue", typeEntityMetadata.getIdentifierField().getFieldName().getSymbolName())
										.addAttribute("path", "/" + getPlural(type.getParameters().get(0)).toLowerCase())
									.build();
					
					FieldMetadata fieldMetadata = beanInfoMetadata.getFieldForPropertyName(paramName);
					if (null != MemberFindingUtils.getAnnotationOfType(fieldMetadata.getAnnotations(), new JavaType("javax.persistence.ManyToMany"))) {
						fieldElement.setAttribute("multiple", "true");
					}
				}
			} else if (isEnumType(type) && null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Enumerated")) && isEnumType(field.getFieldType())) {
				fieldElement = new XmlElementBuilder("field:select", document)
									.addAttribute("items", "${" + getPlural(type).toLowerCase() + "}")
									.addAttribute("path", "/" + getPlural(type).toLowerCase())
								.build();
				
			} else if (type.getFullyQualifiedTypeName().equals(Boolean.class.getName()) || type.getFullyQualifiedTypeName().equals(boolean.class.getName())) {	
				fieldElement = document.createElement("field:checkbox");
			} else if (isSpecialType(type)) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					fieldElement = new XmlElementBuilder("field:select", document)
										.addAttribute("items", "${" + getPlural(type).toLowerCase() + "}")
										.addAttribute("itemValue", typeEntityMetadata.getIdentifierField().getFieldName().getSymbolName())
										.addAttribute("path", "/" + getPlural(type).toLowerCase())
									.build();
				}
			} else if (field.getFieldType().getFullyQualifiedTypeName().equals(Date.class.getName()) || field.getFieldType().getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
				fieldElement = new XmlElementBuilder("field:datetime", document)
									.addAttribute("dateTimePattern", "${" + entityName + "_" + paramName.getSymbolName().toLowerCase() + "_date_format}")
								.build();

			} else {	
				fieldElement = document.createElement("field:input");
			}
			
			addCommonAttributes(field, fieldElement); 
			fieldElement.setAttribute("disableFormBinding", "true");
			fieldElement.setAttribute("field", paramName.getSymbolName());
			fieldElement.setAttribute("id", "f:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + paramName);
			fieldElement.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(fieldElement));

			formFind.appendChild(fieldElement);
		}			
		return document;
	}	
	
	private void createFieldsForCreateAndUpdate(Document document, Element root, String createOrUpdate) {		
		for (FieldMetadata field : fields) {
			String fieldName = field.getFieldName().getSymbolName();
			JavaType fieldType = field.getFieldType();
			List<AnnotationMetadata> annotations = field.getAnnotations();
			AnnotationMetadata annotationMetadata;
			
			//ignoring java.util.Map field types (see ROO-194)
			if (fieldType.equals(new JavaType(Map.class.getName()))) {
				continue;
			}
			if (fieldType.getFullyQualifiedTypeName().equals(Set.class.getName())) {
				if (fieldType.getParameters().size() != 1) {
					throw new IllegalArgumentException("A set is defined without specification of its type (via generics) - unable to create view for it");
				}
				fieldType = fieldType.getParameters().get(0);
			}
			Element fieldElement = null; 
					
			//handle boolean fields
			if (fieldType.getFullyQualifiedTypeName().equals(Boolean.class.getName()) || fieldType.getFullyQualifiedTypeName().equals(boolean.class.getName())) {
				 fieldElement = document.createElement("field:checkbox");
			//handle enum fields	 
			} else if (null != MemberFindingUtils.getAnnotationOfType(annotations, new JavaType("javax.persistence.Enumerated")) && isEnumType(fieldType)) {
				fieldElement = new XmlElementBuilder("field:select", document).addAttribute("items", "${" + getPlural(fieldType).toLowerCase() + "}").addAttribute("path", getPlural(fieldType).toLowerCase()).build();
			} else {
				for (AnnotationMetadata annotation : annotations) {
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToMany")) {
						//OneToMany relationships are managed from the 'many' side of the relationship, therefore we provide a link to the relevant form
						//the link URL is determined as a best effort attempt following Roo REST conventions, this link might be wrong if custom paths are used
						//if custom paths are used the developer can adjust the path attribute in the field:reference tag accordingly
						EntityMetadata typeEntityMetadata = getEntityMetadataForField(field);
						if (typeEntityMetadata != null) {
							fieldElement = new XmlElementBuilder("field:simple", document).addAttribute("messageCode", "entity.reference.not.managed").addAttribute("messageCodeAttribute", beanInfoMetadata.getJavaBean().getSimpleTypeName()).build();
						}
					}
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToMany")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToOne")) {

						JavaType referenceType = getJavaTypeForField(field);
						if (referenceType != null) {
							fieldElement = new XmlElementBuilder("field:select", document)
													.addAttribute("items", "${" + getPlural(referenceType).toLowerCase() + "}")
													.addAttribute("itemValue", getEntityMetadataForField(field).getIdentifierField().getFieldName().getSymbolName())
													.addAttribute("path", "/" + getPlural(referenceType).toLowerCase())
												.build();
											
							if(annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToMany")) {
								fieldElement.setAttribute("multiple", "true");
							}
						}
					} 
					// only include the date picker for styles supported by Dojo (SMALL & MEDIUM)
					if (fieldType.getFullyQualifiedTypeName().equals(Date.class.getName()) || fieldType.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
							fieldElement = new XmlElementBuilder("field:datetime", document).addAttribute("dateTimePattern", "${" + entityName + "_" + fieldName.toLowerCase() + "_date_format}").build();
						
						if (null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future"))) {
							fieldElement.setAttribute("future", "true");
						} else if (null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past"))) {
							fieldElement.setAttribute("past", "true");
						}
						
//						AnnotationMetadata annotation = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("org.springframework.format.annotation.DateTimeFormat"));
//						if (annotation != null) {
//							AnnotationAttributeValue<?> value = annotation.getAttribute(new JavaSymbolName("style"));
//							if (null != value && !value.getValue().toString().contains("L") && !value.getValue().toString().contains("F")) {
//								//dojo can not deal with any other format
//							}
//						}
					}
				}
				if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Size")))) {
					AnnotationAttributeValue<?> max = annotationMetadata.getAttribute(new JavaSymbolName("max"));
					if(max != null) {
						int maxValue = (Integer)max.getValue();
						if (fieldElement == null && maxValue > 30) {	
							fieldElement = new XmlElementBuilder("field:textarea", document).build();
						} 
					}
				} 
			}
			//use a default input field if no other criteria apply
			if (fieldElement == null) {
				fieldElement = document.createElement("field:input");
			}

			addCommonAttributes(field, fieldElement); 
			fieldElement.setAttribute("field", fieldName);
			fieldElement.setAttribute("id", "c:" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + field.getFieldName().getSymbolName());
			fieldElement.setAttribute("z", XmlRoundTripUtils.calculateUniqueKeyFor(fieldElement));
			
			root.appendChild(fieldElement);
		}
	}
	
	private JavaType getJavaTypeForField(FieldMetadata field) {
		if (field.getFieldType().isCommonCollectionType()) {
			//currently there is no scaffolding available for Maps (see ROO-194)
			if(field.getFieldType().equals(new JavaType(Map.class.getName()))) {
				return null;
			}
			List<JavaType> parameters = field.getFieldType().getParameters();
			if (parameters.size() == 0) {
				throw new IllegalStateException("Could not determine the parameter type for the " + field.getFieldName().getSymbolName() + " field in " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
			}
			return parameters.get(0);
		} else {
			return field.getFieldType();
		}
	}

	private EntityMetadata getEntityMetadataForField(FieldMetadata field) {
		return (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(getJavaTypeForField(field), Path.SRC_MAIN_JAVA));
	}

	private void addCommonAttributes(FieldMetadata field, Element fieldElement) {
		AnnotationMetadata annotationMetadata;
		if(field.getFieldType().equals(new JavaType(Integer.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(int.class.getName())
				|| field.getFieldType().equals(new JavaType(Short.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(short.class.getName())
				|| field.getFieldType().equals(new JavaType(Long.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(long.class.getName())
				|| field.getFieldType().equals(new JavaType("java.math.BigInteger"))) {
			fieldElement.setAttribute("validationMessageCode", "field.invalid.integer");
		} else if (StringUtils.uncapitalize(field.getFieldName().getSymbolName()).contains("email")) {
			fieldElement.setAttribute("validationMessageCode", "field.invalid.email");
		} else if(field.getFieldType().equals(new JavaType(Double.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(double.class.getName())
				|| field.getFieldType().equals(new JavaType(Float.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(float.class.getName())
				|| field.getFieldType().equals(new JavaType("java.math.BigDecimal"))) {
			fieldElement.setAttribute("validationMessageCode", "field.invalid.number");
		}
		if ("field:input".equals(fieldElement.getTagName()) && null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Min")))) {
			AnnotationAttributeValue<?> min = annotationMetadata.getAttribute(new JavaSymbolName("value"));
			if(min != null) {
				fieldElement.setAttribute("min", min.getValue().toString());
			}
		}
		if ("field:input".equals(fieldElement.getTagName()) && null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Max")))
				&& !"field:textarea".equals(fieldElement.getTagName())) {
			AnnotationAttributeValue<?> maxA = annotationMetadata.getAttribute(new JavaSymbolName("value"));
			if(maxA != null) {
				fieldElement.setAttribute("max", maxA.getValue().toString());
			}
		}
		if ("field:input".equals(fieldElement.getTagName()) && null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMin")))
				&& !"field:textarea".equals(fieldElement.getTagName())) {
			AnnotationAttributeValue<?> decimalMin = annotationMetadata.getAttribute(new JavaSymbolName("value"));
			if(decimalMin != null) {
				fieldElement.setAttribute("decimalMin", decimalMin.getValue().toString());
			}
		}
		if ("field:input".equals(fieldElement.getTagName()) && null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMax")))) {
			AnnotationAttributeValue<?> decimalMax = annotationMetadata.getAttribute(new JavaSymbolName("value"));
			if(decimalMax != null) {
				fieldElement.setAttribute("decimalMax", decimalMax.getValue().toString());
			}
		}
		if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Pattern")))) {
			AnnotationAttributeValue<?> regexp = annotationMetadata.getAttribute(new JavaSymbolName("regexp"));
			if(regexp != null) {
				fieldElement.setAttribute("validationRegex", regexp.getValue().toString());
			}
		}
		if ("field:input".equals(fieldElement.getTagName()) && null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Size")))) {
			AnnotationAttributeValue<?> max = annotationMetadata.getAttribute(new JavaSymbolName("max"));
			if(max != null) {
				fieldElement.setAttribute("max", max.getValue().toString());
			}
			AnnotationAttributeValue<?> min = annotationMetadata.getAttribute(new JavaSymbolName("min"));
			if(min != null) {
				fieldElement.setAttribute("min", min.getValue().toString());
			}
		}
		if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.NotNull")))) {
			String tagName = fieldElement.getTagName();
			if (tagName.endsWith("textarea") || tagName.endsWith("input") || tagName.endsWith("datetime") || tagName.endsWith("textarea") ||	tagName.endsWith("select") || tagName.endsWith("reference")) {
				fieldElement.setAttribute("required", "true");
			}
		}
	}
	
	private boolean isEnumType(JavaType type) {
		PhysicalTypeMetadata physicalTypeMetadata  = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifierNamingUtils.createIdentifier(PhysicalTypeIdentifier.class.getName(), type, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata != null) {
			PhysicalTypeDetails details = physicalTypeMetadata.getPhysicalTypeDetails();
			if (details != null) {
				if (details.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isSpecialType(JavaType javaType) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		//we are only interested if the type is part of our application and if no editor exists for it already
		if (metadataService.get(physicalTypeIdentifier) != null) {
		  return true;
		}		
		return false;
	}
	
	private String getPlural(JavaType type) {
		if (pluralCache.get(type) != null) {
			return pluralCache.get(type);
		}
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine the plural for the '" + type.getFullyQualifiedTypeName() + "' type");
		if (!pluralMetadata.getPlural().equals(type.getSimpleTypeName())) {
			pluralCache.put(type, pluralMetadata.getPlural());
			return pluralMetadata.getPlural();
		}
		pluralCache.put(type, pluralMetadata.getPlural() + "Items");
		return pluralMetadata.getPlural() + "Items";	
	}
}
