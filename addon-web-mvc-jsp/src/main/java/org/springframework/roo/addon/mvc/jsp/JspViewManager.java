package org.springframework.roo.addon.mvc.jsp;

import java.beans.Introspector;
import java.util.Calendar;
import java.util.Date;
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
	private final String entityNamePlural;
	private final String controllerPath;
	
	public JspViewManager(MetadataService metadataService, List<FieldMetadata> fields, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FinderMetadata finderMetadata, WebScaffoldAnnotationValues webScaffoldAnnotationValues) {
		Assert.notNull(fields, "List of fields required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.notNull(finderMetadata, "Finder metadata required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(webScaffoldAnnotationValues, "Web scaffold annotation values required");
		this.fields = fields;
		
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.metadataService = metadataService;
		this.finderMetadata = finderMetadata;
		this.webScaffoldAnnotationValues = webScaffoldAnnotationValues;

		entityName = StringUtils.uncapitalize(beanInfoMetadata.getJavaBean().getSimpleTypeName());
		
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(beanInfoMetadata.getJavaBean(), Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entityName + "'");
		entityNamePlural = pluralMetadata.getPlural().toLowerCase();
		
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
								.addAttribute("xmlns:field", "urn:jsptagdir:/WEB-INF/tags/form/fields")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build();
		document.appendChild(div);
		
		//create field:table element
		StringBuilder fieldNames = new StringBuilder(6);
		StringBuilder readableFieldNames = new StringBuilder(6);
		
		int fieldCounter = 0;
		for (FieldMetadata field : fields) {
			if (fieldCounter > 0) {
				fieldNames.append(",");
				readableFieldNames.append(",");
			}
			if(++fieldCounter < 7) {
				fieldNames.append(field.getFieldName().getSymbolName());
				readableFieldNames.append(field.getFieldName().getReadableSymbolName());
			}
		}
		
		Element fieldTable = new XmlElementBuilder("field:table", document)
								.addAttribute("id", "field:table_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("data", "${" + entityNamePlural + "}")
								.addAttribute("columns", fieldNames.toString())
								.addAttribute("columnHeadings", readableFieldNames.toString())
							.build();
		fieldTable.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(fieldTable)));
		
		if (!webScaffoldAnnotationValues.isUpdate()) {
			fieldTable.setAttribute("update", "false");
		}
		if (!webScaffoldAnnotationValues.isDelete()) {
			fieldTable.setAttribute("delete", "false");
		}
		if (!controllerPath.toLowerCase().equals(beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase())) {
			fieldTable.setAttribute("customPath", controllerPath);
		}
		
		//create page:list element
		Element pageList = new XmlElementBuilder("page:list", document)
								.addAttribute("id", "list_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("items", "${" + entityNamePlural + "}")
								.addChild(fieldTable)
							.build();
		
		pageList.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(pageList)));

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
								.addAttribute("id", "show_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("object", "${" + entityName + "}")
							.build();
		pageShow.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(pageShow)));

		
		//add field:display elements for each field
		for (FieldMetadata field : fields) {
			String fieldName = Introspector.decapitalize(StringUtils.capitalize(field.getFieldName().getSymbolName()));
			Element fieldDisplay = new XmlElementBuilder("field:display", document)
								.addAttribute("id", "field:display_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + field.getFieldName().getSymbolName())
								.addAttribute("object", "${" + entityName + "}")
								.addAttribute("field", fieldName)
							.build();
			if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				fieldDisplay.setAttribute("date", "true");
				fieldDisplay.setAttribute("dateTimePattern", "${" + entityName + "_" + fieldName + "_date_format}");
			} else if (field.getFieldType().equals(new JavaType(Calendar.class.getName()))) {
				fieldDisplay.setAttribute("calendar", "true");
				fieldDisplay.setAttribute("dateTimePattern", "${" + entityName + "_" + fieldName + "_date_format}");
			}
			fieldDisplay.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(fieldDisplay)));

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
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build());

		//add form create element
		Element formCreate = new XmlElementBuilder("form:create", document)
						.addAttribute("id", "create_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
						.addAttribute("object", "${" + entityName + "}")
						.addAttribute("path", controllerPath)
					.build();
		formCreate.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(formCreate)));

		if (!controllerPath.toLowerCase().equals(beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase())) {
			formCreate.setAttribute("path", controllerPath);
		}
		
		createFieldsForCreateAndUpdate(document, formCreate, "create");
		
		div.appendChild(formCreate);

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
						.addAttribute("id", "update_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
						.addAttribute("object", "${" + entityName + "}")
					.build();	
		formUpdate.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(formUpdate)));

		
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
								.addAttribute("id", "find_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName())
								.addAttribute("objectName", entityName)
								.addAttribute("finderName", finderName.replace("find" + entityMetadata.getPlural(), ""))
							.build();
		formFind.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(formFind)));

		
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
			Assert.notNull(field, "could not find field '" + paramName + "' in '" + type.getFullyQualifiedTypeName() + "'");
			Element fieldElement = null;

			if (type.isCommonCollectionType() && isSpecialType(type.getParameters().get(0))) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type.getParameters().get(0), Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					fieldElement = new XmlElementBuilder("field:select", document)
										.addAttribute("items", "${" + typeEntityMetadata.getPlural().toLowerCase() + "}")
										.addAttribute("itemValue", typeEntityMetadata.getIdentifierField().getFieldName().getSymbolName())
									.build();
					
					FieldMetadata fieldMetadata = beanInfoMetadata.getFieldForPropertyName(paramName);
					if (null != MemberFindingUtils.getAnnotationOfType(fieldMetadata.getAnnotations(), new JavaType("javax.persistence.ManyToMany"))) {
						fieldElement.setAttribute("multiple", "true");
					}
				}
			} else if (isEnumType(type) && null != MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Enumerated")) && isEnumType(field.getFieldType())) {
				PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(field.getFieldType(), Path.SRC_MAIN_JAVA));
				Assert.notNull(pluralMetadata, "Could not determine the plural for the '" + field.getFieldType().getFullyQualifiedTypeName() + "' type");
				fieldElement = new XmlElementBuilder("field:select", document)
									.addAttribute("items", "${" + pluralMetadata.getPlural().toLowerCase() + "}")
								.build();
				
			} else if (type.getFullyQualifiedTypeName().equals(Boolean.class.getName()) || type.getFullyQualifiedTypeName().equals(boolean.class.getName())) {	
				fieldElement = document.createElement("field:checkbox");
			} else if (isSpecialType(type)) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					fieldElement = new XmlElementBuilder("field:select", document)
										.addAttribute("items", "${" + typeEntityMetadata.getPlural().toLowerCase() + "}")
										.addAttribute("itemValue", typeEntityMetadata.getIdentifierField().getFieldName().getSymbolName())
									.build();
				}
			} else if (field.getFieldType().getFullyQualifiedTypeName().equals(Date.class.getName()) || field.getFieldType().getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
				fieldElement = new XmlElementBuilder("field:datetime", document)
									.addAttribute("dateTimePattern", "${" + entityName + "_" + paramName + "_date_format}")
								.build();

			} else {	
				fieldElement = document.createElement("field:input");
			}
			addCommonAttributes(field, fieldElement); 
			fieldElement.setAttribute("disableFormBinding", "true");
			fieldElement.setAttribute("field", paramName.getSymbolName());
			fieldElement.setAttribute("objectName", entityName);
			fieldElement.setAttribute("id", "find_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + paramName);
			fieldElement.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(fieldElement)));

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
			} else if (null != MemberFindingUtils.getAnnotationOfType(annotations, new JavaType("javax.persistence.Enumerated")) && isEnumType(field.getFieldType())) {
				PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(field.getFieldType(), Path.SRC_MAIN_JAVA));
				Assert.notNull(pluralMetadata, "Could not determine the plural for the '" + field.getFieldType().getFullyQualifiedTypeName() + "' type");
				fieldElement = new XmlElementBuilder("field:select", document).addAttribute("items", "${" + pluralMetadata.getPlural().toLowerCase() + "}").build();
			} else {
				for (AnnotationMetadata annotation : annotations) {
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToMany")) {
						//OneToMany relationships are managed from the 'many' side of the relationship, therefore we provide a link to the relevant form
						fieldElement = new XmlElementBuilder("field:reference", document).addAttribute("targetName", fieldType.getSimpleTypeName()).build();
					}
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToMany")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToOne")) {

						EntityMetadata typeEntityMetadata = null;
						
						if (field.getFieldType().isCommonCollectionType()) {
							//currently there is no scaffolding available for Maps (see ROO-194)
							if(field.getFieldType().equals(new JavaType(Map.class.getName()))) {
								return;
							}
							List<JavaType> parameters = field.getFieldType().getParameters();
							if (parameters.size() == 0) {
								throw new IllegalStateException("Could not determine the parameter type for the " + fieldName + " field in " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
							}
							typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(parameters.get(0), Path.SRC_MAIN_JAVA));
						} else {
							typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(field.getFieldType(), Path.SRC_MAIN_JAVA));
						}
						Assert.notNull(typeEntityMetadata, "Could not determine the plural name for the '" + Introspector.decapitalize(StringUtils.capitalize(field.getFieldName().getSymbolName())) + "' field in " + beanInfoMetadata.getJavaBean().getSimpleTypeName());						
						fieldElement = new XmlElementBuilder("field:select", document)
												.addAttribute("items", "${" + typeEntityMetadata.getPlural().toLowerCase() + "}")
												.addAttribute("itemValue", typeEntityMetadata.getIdentifierField().getFieldName().getSymbolName())
											.build();
										
						if(annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToMany")) {
							fieldElement.setAttribute("multiple", "true");
						}
					} 
					// only include the date picker for styles supported by Dojo (SMALL & MEDIUM)
					if (fieldType.getFullyQualifiedTypeName().equals(Date.class.getName()) || fieldType.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
							fieldElement = new XmlElementBuilder("field:datetime", document).addAttribute("dateTimePattern", "${" + entityName + "_" + fieldName + "_date_format}").build();
						
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
							fieldElement = new XmlElementBuilder("field:textarea", document).addAttribute("max", max.getValue().toString()).build();
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
			fieldElement.setAttribute("id", createOrUpdate + "_" + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + field.getFieldName().getSymbolName());
			fieldElement.setAttribute("z", XmlUtils.base64(XmlUtils.sha1Element(fieldElement)));

			root.appendChild(fieldElement);
		}
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
		if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Min")))) {
			AnnotationAttributeValue<?> min = annotationMetadata.getAttribute(new JavaSymbolName("value"));
			if(min != null) {
				fieldElement.setAttribute("min", min.getValue().toString());
			}
		}
		if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Max")))) {
			AnnotationAttributeValue<?> maxA = annotationMetadata.getAttribute(new JavaSymbolName("value"));
			if(maxA != null) {
				fieldElement.setAttribute("max", maxA.getValue().toString());
			}
		}
		if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMin")))) {
			AnnotationAttributeValue<?> decimalMin = annotationMetadata.getAttribute(new JavaSymbolName("value"));
			if(decimalMin != null) {
				fieldElement.setAttribute("decimalMin", decimalMin.getValue().toString());
			}
		}
		if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.DecimalMax")))) {
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
		if (null != (annotationMetadata = MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Size")))) {
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
			if ("field:textarea".equals(tagName) || 
					"field:input".equals(tagName) || 
					"field:datetime".equals(tagName) ||
					"field:textarea".equals(tagName) ||
					"field:select".equals(tagName)) {
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
}