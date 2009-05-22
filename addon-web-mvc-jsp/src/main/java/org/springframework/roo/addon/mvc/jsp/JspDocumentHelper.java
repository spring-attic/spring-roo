package org.springframework.roo.addon.mvc.jsp;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.jvnet.inflector.Noun;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
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
	public JspDocumentHelper(MetadataService metadataService, List<FieldMetadata> fields, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, String projectName) {
		Assert.notNull(fields, "List of fields required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		Assert.hasText(projectName, "Project name required");
		Assert.notNull(metadataService, "Metadata service required");
		this.fields = fields;
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		this.projectName = projectName;
		this.metadataService = metadataService;
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
	
	private Document getListContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element root = (Element) document.getFirstChild();		
		
		Element scriptElement = document.createElement("script");
		scriptElement.setAttribute("type", "text/javascript");
		scriptElement.setTextContent("dojo.require(\"dijit.TitlePane\");");
		root.appendChild(scriptElement);
		
		Element divElement = document.createElement("div");
		divElement.setAttribute("dojoType", "dijit.TitlePane");
		divElement.setAttribute("title", "List all " + entityMetadata.getPlural());

		divElement.setAttribute("style", "width: 100%");		

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
				fmt.setAttribute("pattern", "MM/dd/yyyy");
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
		root.appendChild(divElement);
		
		return document;
	}	

	private Document getShowContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element root = (Element) document.getFirstChild();		
		
		Element scriptElement = document.createElement("script");
		scriptElement.setAttribute("type", "text/javascript");
		scriptElement.setTextContent("dojo.require(\"dijit.TitlePane\");");
		root.appendChild(scriptElement);
		
		Element divElement = document.createElement("div");
		divElement.setAttribute("dojoType", "dijit.TitlePane");
		divElement.setAttribute("title", "Show " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		divElement.setAttribute("style", "width: 100%");	
		
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
				fmt.setAttribute("pattern", "MM/dd/yyyy");
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
		
		root.appendChild(divElement);
		
		return document;
	}
	
	private Document getCreateContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element root = (Element) document.getFirstChild();		
		
		Element scriptElement = document.createElement("script");
		scriptElement.setAttribute("type", "text/javascript");
		scriptElement.setTextContent("dojo.require(\"dijit.TitlePane\");");
		root.appendChild(scriptElement);
		
		Element divElement = document.createElement("div");
		divElement.setAttribute("dojoType", "dijit.TitlePane");
		divElement.setAttribute("title", "Create New " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		divElement.setAttribute("style", "width: 100%");		

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
		divSubmitElement.appendChild(getSubmitButtonDojo(document, "proceed"));
		divSubmitElement.appendChild(inputElement);
		formElement.appendChild(divSubmitElement);

		divElement.appendChild(formElement);
		root.appendChild(divElement);	

		return document;
	}
	
	private Document getUpdateContent(Document document) {
		Assert.notNull(document, "Document required");
		
		String entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Element root = (Element) document.getFirstChild();

		Element scriptElement = document.createElement("script");
		scriptElement.setAttribute("type", "text/javascript");
		scriptElement.setTextContent("dojo.require(\"dijit.TitlePane\");");
		root.appendChild(scriptElement);
		
		Element divElement = document.createElement("div");
		divElement.setAttribute("dojoType", "dijit.TitlePane");
		divElement.setAttribute("title", "Update " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		divElement.setAttribute("style", "width: 100%");	

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
		divSubmitElement.appendChild(getSubmitButtonDojo(document, "proceed"));
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
		root.appendChild(divElement);
	
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
				
				Element formCheck = document.createElement("form:checkbox");
				formCheck.setAttribute("path", field.getFieldName().getSymbolName());
				formCheck.setAttribute("id", "_" + field.getFieldName().getSymbolName());
				divElement.appendChild(formCheck);
				formElement.appendChild(divElement);
				formElement.appendChild(document.createElement("br"));
			} else {
				boolean specialAnnotation = false;
				for (AnnotationMetadata annotation : field.getAnnotations()) {
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToMany")) {

						//TODO: direct dependency on Inflector should be removed
						String plural = Noun.pluralOf(field.getFieldType().getSimpleTypeName()).toLowerCase();
						Element ifElement = document.createElement("c:if");
						ifElement.setAttribute("test", "${not empty " + plural + "}");
						divElement.appendChild(ifElement);
						
						divElement.removeChild(labelElement);
						ifElement.appendChild(labelElement);
						
						ifElement.appendChild(getSelectBox(document, field, plural));		

						specialAnnotation = true;
						formElement.appendChild(divElement);
						formElement.appendChild(document.createElement("br"));
						
						if(annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")) {
							ifElement.appendChild(getSelectDojo(document, field));
						} else {
							ifElement.appendChild(getMultiSelectDojo(document, field));
						}
					} else if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.validation.constraints.Size")) {
						AnnotationAttributeValue<?> max = annotation.getAttribute(new JavaSymbolName("max"));
						if(max != null) {
							int maxValue = (Integer)max.getValue();
							if(maxValue > 30) {
								divElement.appendChild(getTextArea(document, field, maxValue));
								divElement.appendChild(getTextAreaDojo(document, field));								
							} else {
								divElement.appendChild(getInputBox(document, field, maxValue));
							}
							divElement.appendChild(document.createElement("br"));
							divElement.appendChild(getErrorsElement(document, field));
							divElement.appendChild(getValidationDojo(document, field));
							formElement.appendChild(divElement);
							formElement.appendChild(document.createElement("br"));	
							specialAnnotation = true;
						}
					} else if (isEnumType(field.getFieldType())) {
						divElement.appendChild(getEnumSelectBox(document, field));		
						divElement.appendChild(getSelectDojo(document, field));
						divElement.appendChild(document.createElement("br"));
						divElement.appendChild(getErrorsElement(document, field));
						formElement.appendChild(divElement);
						formElement.appendChild(document.createElement("br"));	
						specialAnnotation = true;
					}
				}
				if (!specialAnnotation) {
					divElement.appendChild(getInputBox(document, field, 30));
					divElement.appendChild(document.createElement("br"));
					divElement.appendChild(getErrorsElement(document, field));
					divElement.appendChild(getValidationDojo(document, field));
					
					if (fieldType.getFullyQualifiedTypeName().equals(Date.class.getName()) ||
							// should be tested with instanceof
									fieldType.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
								divElement.appendChild(getDateDojo(document, field, "MM/dd/yyyy"));
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
	
	private Element getDateDojo(Document document, FieldMetadata field, String pattern) {
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + field.getFieldName().getSymbolName()
				+ "\", widgetType : \"dijit.form.DateTextBox\", widgetAttrs : {datePattern : \"" + pattern + "\", required : "
				+ (isTypeInAnnotationList(new JavaType("javax.validation.NotNull"), field.getAnnotations()) ? "true" : "false") + "}})); ");
		return script;
	}

	private Element getValidationDojo(Document document, FieldMetadata field) {
		String regex = "";
		String invalid = "";
		int min = getMinSize(field);
		int max = getMaxSize(field);
		boolean isRequired = isTypeInAnnotationList(new JavaType("javax.validation.constraints.NotNull"), field.getAnnotations());
		if(field.getFieldType().equals(new JavaType(Integer.class.getName()))) {			
			if (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE) {
				regex = ", regExp: \"-?[0-9]{" + (min == Integer.MIN_VALUE ? "1," : min) + (max == Integer.MAX_VALUE ? "" : max) + "}\"";
			} else {
				regex = ", regExp: \"-?[0-9]*\"";
			}
			invalid = "Integer numbers only";
		} else if(field.getFieldType().equals(new JavaType(Double.class.getName())) || field.getFieldType().equals(new JavaType(Float.class.getName()))) {			
			if (min != Integer.MIN_VALUE || max != Integer.MAX_VALUE) {
				regex = ", regExp: \"-?[0-9]{" + (min == Integer.MIN_VALUE ? "1," : min) + (max == Integer.MAX_VALUE ? "" : max) + "}(\\.?[0-9]*)?\"";
			} else {
				regex = ", regExp: \"-?[0-9]*\\.[0-9]*\"";
			}
			invalid = "Number with '-' or '.' allowed" + (isRequired ? ", required" : "");
		} else if (field.getFieldName().getSymbolName().contains("email")) {
			regex = ", regExp: \"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\"";
			invalid = "Valid email only";
		}	
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");
		String message = "Enter " + field.getFieldName().getReadableSymbolName() + (isRequired ? " (required)" : "");
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + field.getFieldName().getSymbolName()
				+ "\", widgetType : \"dijit.form.ValidationTextBox\", widgetAttrs : {promptMessage: \"" + message + "\", invalidMessage: \"" + invalid 
				+ "\"" + regex + ", required : " + isRequired + "}})); ");
		return script;
	}
	
	private Element getTextAreaDojo(Document document, FieldMetadata field) {
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + field.getFieldName().getSymbolName()
				+ "\", widgetType: \"dijit.form.Textarea\", widgetAttrs: {value: \"\"}})); ");
		return script;
	}
	
	private Element getSelectDojo(Document document, FieldMetadata field) {
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + field.getFieldName().getSymbolName()
				+ "\", widgetType: \"dijit.form.FilteringSelect\", widgetAttrs : {hasDownArrow : true}})); ");
		return script;
	}
	
	private Element getMultiSelectDojo(Document document, FieldMetadata field) {
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + field.getFieldName().getSymbolName()
				+ "\", widgetType: \"dijit.form.MultiSelect\")); ");
		return script;
	}
	
	private Element getSubmitButtonDojo(Document document, String buttonId) {
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ValidateAllDecoration({elementId:'" + buttonId + "', event:'onclick'}));");
		return script;
	}
	
	private Element getInputBox(Document document, FieldMetadata field, Integer maxValue) {
		Element formInput = document.createElement("form:input");
		formInput.setAttribute("path", field.getFieldName().getSymbolName());
		formInput.setAttribute("id", "_" + field.getFieldName().getSymbolName());
		formInput.setAttribute("size", "0");
		formInput.setAttribute("cssStyle", "width:250px");
		formInput.setAttribute("maxlength", maxValue.toString());
		return formInput;
	}
	
	private Element getSelectBox(Document document, FieldMetadata field, String pluralName) {
		Element formSelect = document.createElement("form:select");
		formSelect.setAttribute("path", field.getFieldName().getSymbolName());
		formSelect.setAttribute("cssStyle", "width:250px");						
		formSelect.setAttribute("id", "_" + field.getFieldName().getSymbolName());
		Element formOptions = document.createElement("form:options");
		formOptions.setAttribute("items", "${" + pluralName.toLowerCase() + "}");
		formOptions.setAttribute("itemValue", "id");	
		formSelect.appendChild(formOptions);		
		return formSelect;
	}
	
	private Element getEnumSelectBox(Document document, FieldMetadata field) {
		Element formSelect = document.createElement("form:select");
		formSelect.setAttribute("path", field.getFieldName().getSymbolName());
		formSelect.setAttribute("cssStyle", "width:250px");						
		formSelect.setAttribute("id", "_" + field.getFieldName().getSymbolName());
		formSelect.setAttribute("items", "${_" + field.getFieldName().getSymbolName() + "}");		
		return formSelect;
	}
	
	private Element getTextArea(Document document, FieldMetadata field, Integer maxValue) {
		Element textArea = document.createElement("form:textarea");
		textArea.setAttribute("path", field.getFieldName().getSymbolName());
		textArea.setAttribute("id", "_" + field.getFieldName().getSymbolName());
		textArea.setAttribute("cssStyle", "width:250px;height:" + Math.round(new Float(maxValue) / 10) + "em");
		return textArea;
	}
	
	private Element getErrorsElement(Document document, FieldMetadata field) {
		Element errors = document.createElement("form:errors");
		errors.setAttribute("path", field.getFieldName().getSymbolName());
		errors.setAttribute("id", "_" + field.getFieldName().getSymbolName());
		errors.setAttribute("cssClass", "errors");
		return errors;
	}
	
	private boolean isTypeInAnnotationList(JavaType type, List<AnnotationMetadata> annotations) {
		for (AnnotationMetadata annotation : annotations) {
			if(annotation.getAnnotationType().equals(type)) {
				return true;
			}
		}
		return false;
	}
	
	private int getMinSize(FieldMetadata field) {		
		for(AnnotationMetadata annotation : field.getAnnotations()) {
			if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.validation.constraints.Size")) {
				AnnotationAttributeValue<?> minValue = annotation.getAttribute(new JavaSymbolName("min"));
				if(minValue != null) {
					return (Integer)minValue.getValue();
				}
			}			
		}
		return Integer.MIN_VALUE;
	}	
	
	private int getMaxSize(FieldMetadata field) {	
		for(AnnotationMetadata annotation : field.getAnnotations()) {
			if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.validation.constraints.Size")) {
				AnnotationAttributeValue<?> maxValue = annotation.getAttribute(new JavaSymbolName("max"));
				if(maxValue != null) {
					return (Integer)maxValue.getValue();
				}
			}			
		}
		return Integer.MAX_VALUE;
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
}