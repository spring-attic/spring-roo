package org.springframework.roo.addon.mvc.jsp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Helper class which generates the contents of the various jsp documents
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
public class JspDocumentHelper {
	
	private List<FieldMetadata> fields; 
	private BeanInfoMetadata beanInfoMetadata; 
	private EntityMetadata entityMetadata;
	private String projectName;
	private MetadataService metadataService;
	private FinderMetadata finderMetadata;
	private SimpleDateFormat dateFormatLocalized;
	
	public JspDocumentHelper(MetadataService metadataService, List<FieldMetadata> fields, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FinderMetadata finderMetadata, String projectName) {
		Assert.notNull(fields, "List of fields required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.notNull(finderMetadata, "Finder metadata required");
		Assert.hasText(projectName, "Project name required");
		Assert.notNull(metadataService, "Metadata service required");
		this.fields = fields;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.projectName = projectName;
		this.metadataService = metadataService;
		this.finderMetadata = finderMetadata;
		
		dateFormatLocalized = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
	}
	
	public Document getListDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		
		
		document.appendChild(document.createElement("div"));		
		document = addHeaders(document);		
		document = getListContent(document);		
		document = addFooter(document);
		
		return document;
	}
	
	public Document getShowDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		
		
		document.appendChild(document.createElement("div"));		
		document = addHeaders(document);		
		document = getShowContent(document);		
		document = addFooter(document);
		
		return document;
	}
	
	public Document getCreateDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		
		
		document.appendChild(document.createElement("div"));		
		document = addHeaders(document);		
		document = getCreateContent(document);		
		document = addFooter(document);
		
		return document;
	}
	
	public Document getUpdateDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		
		
		document.appendChild(document.createElement("div"));		
		document = addHeaders(document);		
		document = getUpdateContent(document);		
		document = addFooter(document);
		
		return document;
	}
	
	public Document getFinderDocument(String finderName) {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		
		
		document.appendChild(document.createElement("div"));		
		document = addHeaders(document);		
		document = getFinderContent(document, finderName);		
		document = addFooter(document);
		
		return document;
	}
	
	private Document getListContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element divElement = DojoUtils.getTitlePaneDojo(document, "List all " + entityMetadata.getPlural());

		Element ifElement = document.createElement("c:if");
		ifElement.setAttribute("test", "${not empty " + entityMetadata.getPlural().toLowerCase() + "}");
		Element tableElement = document.createElement("table");
		tableElement.setAttribute("width", "300px");
		ifElement.appendChild(tableElement);
		Element trElement = document.createElement("tr");
		tableElement.appendChild(trElement);
		Element theadElement = document.createElement("thead");
		trElement.appendChild(theadElement);

		Element idThElement = document.createElement("th");
		idThElement.setTextContent(entityMetadata.getIdentifierField().getFieldName().getReadableSymbolName());
		theadElement.appendChild(idThElement);
		
		int fieldCounter = 0;
		for (FieldMetadata field : fields) {
			Element thElement = document.createElement("th");
			thElement.setTextContent(field.getFieldName().getReadableSymbolName());
			if(++fieldCounter < 7) {
				theadElement.appendChild(thElement);
			}
		}
		theadElement.appendChild(document.createElement("th"));
		theadElement.appendChild(document.createElement("th"));
		theadElement.appendChild(document.createElement("th"));

		Element forEachElement = document.createElement("c:forEach");
		forEachElement.setAttribute("var", entityName);
		forEachElement.setAttribute("items", "${" + entityMetadata.getPlural().toLowerCase() + "}");
		tableElement.appendChild(forEachElement);
		Element trElement2 = document.createElement("tr");
		forEachElement.appendChild(trElement2);

		Element idTdElement = document.createElement("td");
		idTdElement.setTextContent("${" + entityName + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}");
		trElement2.appendChild(idTdElement);
		
		fieldCounter = 0;
		for (FieldMetadata field : fields) {
			Element tdElement = document.createElement("td");
			if (field.getFieldType().isCommonCollectionType()) {
				tdElement.setTextContent("${fn:length(" + entityName + "." + field.getFieldName().getSymbolName() + ")}");
			} else if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				Element fmt = document.createElement("fmt:formatDate");
				fmt.setAttribute("value", "${" + entityName + "." + field.getFieldName().getSymbolName() + "}");
				fmt.setAttribute("type", "DATE");
				fmt.setAttribute("pattern", dateFormatLocalized.toPattern());
				tdElement.appendChild(fmt);
			} else {
				tdElement.setTextContent("${fn:substring(" + entityName + "." + field.getFieldName().getSymbolName() + ", 0, 10)}");
			}
			if(++fieldCounter < 7) {
				trElement2.appendChild(tdElement);
			}
		}

		Element showElement = document.createElement("td");
		Element showFormElement = document.createElement("form:form");
		showFormElement.setAttribute("action", "/" + projectName + "/" + entityName + "/${" + entityName + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}");
		showFormElement.setAttribute("method", "GET");
		Element showSubmitElement = document.createElement("input");
		showSubmitElement.setAttribute("type", "image");
		showSubmitElement.setAttribute("title", "Show " + entityName);
		showSubmitElement.setAttribute("src", "/" + projectName + "/static/images/show.png");
		showSubmitElement.setAttribute("value", "Show " + entityName);
		showSubmitElement.setAttribute("alt", "Show " + entityName);
		showFormElement.appendChild(showSubmitElement);
		showElement.appendChild(showFormElement);
		trElement2.appendChild(showElement);

		Element updateElement = document.createElement("td");		
		Element updateFormElement = document.createElement("form:form");
		updateFormElement.setAttribute("action", "/" + projectName + "/" + entityName + "/${" + entityName + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}/form");
		updateFormElement.setAttribute("method", "GET");
		Element updateSubmitElement = document.createElement("input");
		updateSubmitElement.setAttribute("type", "image");
		updateSubmitElement.setAttribute("title", "Update " + entityName);
		updateSubmitElement.setAttribute("src", "/" + projectName + "/static/images/update.png");
		updateSubmitElement.setAttribute("value", "Update " + entityName);
		updateSubmitElement.setAttribute("alt", "Update " + entityName);
		updateFormElement.appendChild(updateSubmitElement);
		updateElement.appendChild(updateFormElement);
		trElement2.appendChild(updateElement);

		Element deleteElement = document.createElement("td");
		Element deleteFormElement = document.createElement("form:form");
		deleteFormElement.setAttribute("action", "/" + projectName + "/" + entityName + "/${" + entityName + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}");
		deleteFormElement.setAttribute("method", "DELETE");
		Element deleteSubmitElement = document.createElement("input");
		deleteSubmitElement.setAttribute("type", "image");
		deleteSubmitElement.setAttribute("title", "Delete " + entityName);
		deleteSubmitElement.setAttribute("src", "/" + projectName + "/static/images/delete.png");
		deleteSubmitElement.setAttribute("value", "Delete " + entityName);
		deleteSubmitElement.setAttribute("alt", "Delete " + entityName);
		deleteFormElement.appendChild(deleteSubmitElement);
		deleteElement.appendChild(deleteFormElement);
		trElement2.appendChild(deleteElement);
		
		Element elseElement = document.createElement("c:if");
		elseElement.setAttribute("test", "${empty " + entityMetadata.getPlural().toLowerCase() + "}");
		elseElement.setTextContent("No " + entityMetadata.getPlural() + " found.");	

		divElement.appendChild(ifElement);
		divElement.appendChild(elseElement);
		document.getDocumentElement().appendChild(divElement);
		
		return document;
	}	

	private Document getShowContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element divElement = DojoUtils.getTitlePaneDojo(document, "Show " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
			
		Element ifElement = document.createElement("c:if");
		ifElement.setAttribute("test", "${not empty " + entityName + "}");
		divElement.appendChild(ifElement);		

		for (FieldMetadata field : fields) {
			Element divSubmitElement = document.createElement("div");
			divSubmitElement.setAttribute("id", "roo_" + entityName + "_" + field.getFieldName().getSymbolName());
				
			Element label = document.createElement("label");
			label.setAttribute("for", "_" + field.getFieldName().getSymbolName());
			label.setTextContent(field.getFieldName().getReadableSymbolName() + ":");
			divSubmitElement.appendChild(label);
			
			Element divContent = document.createElement("div");
			divContent.setAttribute("id", "_" + field.getFieldName().getSymbolName());
			
			if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				Element fmt = document.createElement("fmt:formatDate");
				fmt.setAttribute("value", "${" + entityName + "." + field.getFieldName().getSymbolName() + "}");
				fmt.setAttribute("type", "DATE");
				fmt.setAttribute("pattern", dateFormatLocalized.toPattern());
				divContent.appendChild(fmt);
			} else {
				divContent.setTextContent("${" + entityName + "." + field.getFieldName().getSymbolName() + "}");
			}
			divSubmitElement.appendChild(divContent);
			ifElement.appendChild(divSubmitElement);
			ifElement.appendChild(document.createElement("br"));
		}

		Element elseElement = document.createElement("c:if");
		elseElement.setAttribute("test", "${empty " + entityName + "}");
		elseElement.setTextContent("No " + beanInfoMetadata.getJavaBean().getSimpleTypeName() + " found with this id.");
		divElement.appendChild(elseElement);
		
		document.getDocumentElement().appendChild(divElement);
		
		return document;
	}
	
	private Document getCreateContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element divElement = DojoUtils.getTitlePaneDojo(document, "Create New " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		
		Element formElement = document.createElement("form:form");
		formElement.setAttribute("modelAttribute", entityName);
		formElement.setAttribute("action", "/" + projectName + "/" + entityName);
		formElement.setAttribute("method", "POST");

		createFieldsForCreateAndUpdate(document, formElement);

		Element divSubmitElement = document.createElement("div");
		divSubmitElement.setAttribute("id", "roo_" + entityName + "_submit");
		divSubmitElement.setAttribute("class", "submit");
		
		Element inputElement = document.createElement("input");
		inputElement.setAttribute("type", "submit");
		inputElement.setAttribute("value", "Save");
		inputElement.setAttribute("id", "proceed");
		divSubmitElement.appendChild(DojoUtils.getSubmitButtonDojo(document, "proceed"));
		divSubmitElement.appendChild(inputElement);
		formElement.appendChild(divSubmitElement);

		divElement.appendChild(formElement);
		document.getDocumentElement().appendChild(divElement);	
		return document;
	}
	
	private Document getUpdateContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element divElement = DojoUtils.getTitlePaneDojo(document, "Update " + beanInfoMetadata.getJavaBean().getSimpleTypeName());

		Element formElement = document.createElement("form:form");
		formElement.setAttribute("modelAttribute", entityName);
		formElement.setAttribute("action", "/" + projectName + "/" + entityName + "/${" + entityName	+ "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}");
		formElement.setAttribute("method", "PUT");		

		createFieldsForCreateAndUpdate(document, formElement);
		
		Element divSubmitElement = document.createElement("div");
		divSubmitElement.setAttribute("id", "roo_" + entityName + "_submit");
		divSubmitElement.setAttribute("class", "submit");
		
		Element inputElement = document.createElement("input");
		inputElement.setAttribute("type", "submit");
		inputElement.setAttribute("value", "Update");
		inputElement.setAttribute("id", "proceed");
		divSubmitElement.appendChild(DojoUtils.getSubmitButtonDojo(document, "proceed"));
		divSubmitElement.appendChild(inputElement);
		formElement.appendChild(divSubmitElement);
		
		Element formHiddenId = document.createElement("form:hidden");
		formHiddenId.setAttribute("path", entityMetadata.getIdentifierField().getFieldName().getSymbolName());
		formHiddenId.setAttribute("id", "_" + entityMetadata.getIdentifierField().getFieldName().getSymbolName());
		formElement.appendChild(formHiddenId);
		Element formHiddenVersion = document.createElement("form:hidden");
		formHiddenVersion.setAttribute("path", entityMetadata.getVersionField().getFieldName().getSymbolName());
		formHiddenVersion.setAttribute("id", "_" + entityMetadata.getVersionField().getFieldName().getSymbolName());
		formElement.appendChild(formHiddenVersion);

		divElement.appendChild(formElement);
		document.getDocumentElement().appendChild(divElement);
	
		return document;
	}
	
	private Document getFinderContent(Document document, String finderName) {
		Assert.notNull(document, "Document required");
		Assert.hasText(finderName, "finder name required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element titleDivElement = DojoUtils.getTitlePaneDojo(document, new JavaSymbolName(finderName).getReadableSymbolName());
		
		Element formElement = document.createElement("form:form");
		formElement.setAttribute("action", "/" + projectName + "/" + entityName + "/find/" + finderName.replace("find" + entityMetadata.getPlural(), ""));
		formElement.setAttribute("method", "GET");		

		MethodMetadata methodMetadata = finderMetadata.getDynamicFinderMethod(finderName);
		
		List<JavaType> types = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());
		List<JavaSymbolName> paramNames = methodMetadata.getParameterNames();
		
		for (int i = 0; i < types.size(); i++) {
			
			JavaType type = types.get(i);
			JavaSymbolName paramName = paramNames.get(i);
			
			Element divElement = document.createElement("div");
			divElement.setAttribute("id", "roo_" + entityName + "_" + paramName.getSymbolName().toLowerCase());

			Element labelElement = document.createElement("label");
			labelElement.setAttribute("for", "_" + paramName.getSymbolName().toLowerCase());
			labelElement.setTextContent(paramName.getReadableSymbolName()  + ":");
			
			if (isSpecialType(type)) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					Element ifElement = document.createElement("c:if");
					ifElement.setAttribute("test", "${not empty " + typeEntityMetadata.getPlural().toLowerCase() + "}");
					divElement.appendChild(ifElement);
					ifElement.appendChild(labelElement);

					Element select = document.createElement("select");
					select.setAttribute("style", "width:250px");
					select.setAttribute("name", type.getSimpleTypeName().toLowerCase());
					Element forEach = document.createElement("c:forEach");
					forEach.setAttribute("items", "${" + typeEntityMetadata.getPlural().toLowerCase() + "}");
					forEach.setAttribute("var", type.getSimpleTypeName().toLowerCase());
					select.appendChild(forEach);
					Element option = document.createElement("option");
					option.setAttribute("value", "${" + type.getSimpleTypeName().toLowerCase() + "." + entityMetadata.getIdentifierField().getFieldName() + "}");
					option.setTextContent("${" + type.getSimpleTypeName().toLowerCase() + "}");
					forEach.appendChild(option);
					ifElement.appendChild(select);		
				}
			} else if (isEnumType(type)) {
				divElement.appendChild(labelElement);
				divElement.appendChild(JspUtils.getEnumSelectBox(document, paramName));		
				divElement.appendChild(DojoUtils.getSelectDojo(document, paramName));
				divElement.appendChild(document.createElement("br"));
				formElement.appendChild(divElement);
				formElement.appendChild(document.createElement("br"));	
			} else if (type.getFullyQualifiedTypeName().equals(Boolean.class.getName())
					|| type.getFullyQualifiedTypeName().equals(boolean.class.getName())) {	
				divElement.appendChild(labelElement);
				Element formCheck = document.createElement("input");
				formCheck.setAttribute("type", "checkbox");
				formCheck.setAttribute("id", "_" + paramName.getSymbolName());
				divElement.appendChild(formCheck);
				formElement.appendChild(divElement);
				formElement.appendChild(document.createElement("br"));
			} else {	
				divElement.appendChild(labelElement);
				Element formInput = document.createElement("input");
				formInput.setAttribute("name", paramName.getSymbolName().toLowerCase());
				formInput.setAttribute("id", "_" + paramName.getSymbolName().toLowerCase());
				formInput.setAttribute("size", "0");
				formInput.setAttribute("style", "width:250px");
				divElement.appendChild(formInput);
				divElement.appendChild(DojoUtils.getSimpleValidationDojo(document, paramName));
				
				if (type.getFullyQualifiedTypeName().equals(Date.class.getName()) ||
						// should be tested with instanceof
						type.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
							divElement.appendChild(DojoUtils.getRequiredDateDojo(document, paramName));
				}
			}

			formElement.appendChild(divElement);
			formElement.appendChild(document.createElement("br"));
		}		
		
		Element divSubmitElement = document.createElement("div");
		divSubmitElement.setAttribute("id", "roo_" + entityName + "_submit");
		divSubmitElement.setAttribute("class", "submit");
		
		Element inputElement = document.createElement("input");
		inputElement.setAttribute("type", "submit");
		inputElement.setAttribute("value", "Find");
		inputElement.setAttribute("id", "proceed");
		divSubmitElement.appendChild(DojoUtils.getSubmitButtonDojo(document, "proceed"));
		divSubmitElement.appendChild(inputElement);
		formElement.appendChild(divSubmitElement);

		titleDivElement.appendChild(formElement);
		document.getDocumentElement().appendChild(titleDivElement);
	
		return document;
	}
	
	private void createFieldsForCreateAndUpdate(Document document, Element formElement) {
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		for (FieldMetadata field : fields) {
			
			JavaType fieldType = field.getFieldType();
			if(fieldType.isCommonCollectionType() && fieldType.equals(new JavaType(Set.class.getName()))) {
				if (fieldType.getParameters().size() != 1) {
					throw new IllegalArgumentException();
				}
				fieldType = fieldType.getParameters().get(0);
			}
			
			Element divElement = document.createElement("div");
			divElement.setAttribute("id", "roo_" + entityName + "_" + field.getFieldName().getSymbolName());
						
			Element labelElement = document.createElement("label");
			labelElement.setAttribute("for", "_" + field.getFieldName().getSymbolName());
			labelElement.setTextContent(field.getFieldName().getReadableSymbolName() + ":");
			divElement.appendChild(labelElement);
			
			if (fieldType.getFullyQualifiedTypeName().equals(Boolean.class.getName())
					|| fieldType.getFullyQualifiedTypeName().equals(boolean.class.getName())) {					
				divElement.appendChild(JspUtils.getCheckBox(document, field.getFieldName()));
				formElement.appendChild(divElement);
				formElement.appendChild(document.createElement("br"));
			} else {
				boolean specialAnnotation = false;
				for (AnnotationMetadata annotation : field.getAnnotations()) {
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToMany")) {

						EntityMetadata typeEntityMetadata = null;
						
						if (field.getFieldType().isCommonCollectionType()) {
							typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(field.getFieldType().getParameters().get(0), Path.SRC_MAIN_JAVA));
						} else {
							typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(field.getFieldType(), Path.SRC_MAIN_JAVA));
						}
	
						if(typeEntityMetadata == null) {
							throw new IllegalStateException("Could not determine the plural name for the " + field.getFieldName().getSymbolNameCapitalisedFirstLetter() + " field");
						}
						String plural = typeEntityMetadata.getPlural().toLowerCase();
						
						Element ifElement = document.createElement("c:if");
						ifElement.setAttribute("test", "${not empty " + plural + "}");
						divElement.appendChild(ifElement);
						
						divElement.removeChild(labelElement);
						ifElement.appendChild(labelElement);
						
						ifElement.appendChild(JspUtils.getSelectBox(document, field.getFieldName(), plural));		

						specialAnnotation = true;
						formElement.appendChild(divElement);
						formElement.appendChild(document.createElement("br"));
						
						if(annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")) {
							ifElement.appendChild(DojoUtils.getSelectDojo(document, field.getFieldName()));
						} else {
							ifElement.appendChild(DojoUtils.getMultiSelectDojo(document, field.getFieldName()));
						}
					} else if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.validation.constraints.Size")) {
						AnnotationAttributeValue<?> max = annotation.getAttribute(new JavaSymbolName("max"));
						if(max != null) {
							int maxValue = (Integer)max.getValue();
							if(maxValue > 30) {		
								divElement.appendChild(JspUtils.getTextArea(document, field.getFieldName(), maxValue));
								divElement.appendChild(DojoUtils.getTextAreaDojo(document, field.getFieldName()));
								divElement.appendChild(document.createElement("br"));
								divElement.appendChild(JspUtils.getErrorsElement(document, field.getFieldName()));
								//TODO: due to ROO-85 the validation Dojo element has been removed since it causes problems in conjunction with Textarea
							} else {
								divElement.appendChild(JspUtils.getInputBox(document, field.getFieldName(), maxValue));
								divElement.appendChild(document.createElement("br"));
								divElement.appendChild(JspUtils.getErrorsElement(document, field.getFieldName()));
								divElement.appendChild(DojoUtils.getValidationDojo(document, field));
							}							
							formElement.appendChild(divElement);
							formElement.appendChild(document.createElement("br"));	
							specialAnnotation = true;
						}
					} else if (isEnumType(field.getFieldType())) {
						divElement.appendChild(JspUtils.getEnumSelectBox(document, field.getFieldName()));		
						divElement.appendChild(DojoUtils.getSelectDojo(document, field.getFieldName()));
						divElement.appendChild(document.createElement("br"));
						divElement.appendChild(JspUtils.getErrorsElement(document, field.getFieldName()));
						formElement.appendChild(divElement);
						formElement.appendChild(document.createElement("br"));	
						specialAnnotation = true;
					}
				}
				if (!specialAnnotation) {
					divElement.appendChild(JspUtils.getInputBox(document, field.getFieldName(), 30));
					divElement.appendChild(document.createElement("br"));
					divElement.appendChild(JspUtils.getErrorsElement(document, field.getFieldName()));
					divElement.appendChild(DojoUtils.getValidationDojo(document, field));
					
					if (fieldType.getFullyQualifiedTypeName().equals(Date.class.getName()) ||
							// should be tested with instanceof
									fieldType.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
								divElement.appendChild(DojoUtils.getDateDojo(document, field));
					}
					
					formElement.appendChild(divElement);
					formElement.appendChild(document.createElement("br"));				
				}
			}
		}
	}
	
	private Document addHeaders(Document document) {		
		// this node is just for temporary purpose - it will not be in the final result
		Node documentRoot = document.getFirstChild();

		documentRoot.appendChild(document.createComment("WARNING: This file is maintained by ROO! IT WILL BE OVERWRITTEN unless you specify "
				+ System.getProperty("line.seperator") + "\t@RooWebScaffold(automaticallyMaintainView = false) in the governing controller"));

		Element includeIncludes = document.createElement("jsp:directive.include");
		includeIncludes.setAttribute("file", "/WEB-INF/jsp/includes.jsp");
		documentRoot.appendChild(includeIncludes);

		Element includeHeader = document.createElement("jsp:directive.include");
		includeHeader.setAttribute("file", "/WEB-INF/jsp/header.jsp");
		documentRoot.appendChild(includeHeader);	

		return document;
	}	

	private Document addFooter(Document document) {
		Element includeFooter = document.createElement("jsp:directive.include");
		includeFooter.setAttribute("file", "/WEB-INF/jsp/footer.jsp");
		document.getFirstChild().appendChild(includeFooter);
		return document;
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