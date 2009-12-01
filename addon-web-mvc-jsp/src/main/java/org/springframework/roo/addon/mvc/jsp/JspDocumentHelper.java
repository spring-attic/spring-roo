package org.springframework.roo.addon.mvc.jsp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.finder.FinderMetadata;
import org.springframework.roo.addon.web.mvc.controller.WebScaffoldAnnotationValues;
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
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	private MetadataService metadataService;
	private FinderMetadata finderMetadata;
	private SimpleDateFormat dateFormatLocalized;
	private WebScaffoldAnnotationValues webScaffoldAnnotationValues;
	private final String entityName;
	private final String controllerPath;
	
	private final String warning = "WARNING: This file is maintained by ROO! IT WILL BE OVERWRITTEN unless you specify "
		+ System.getProperty("line.seperator") + "\t@RooWebScaffold(automaticallyMaintainView = false) in the governing controller";
	
	public JspDocumentHelper(MetadataService metadataService, List<FieldMetadata> fields, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FinderMetadata finderMetadata, WebScaffoldAnnotationValues webScaffoldAnnotationValues) {
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
		
		dateFormatLocalized = webScaffoldAnnotationValues.getDateFormat();
		entityName = beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase();
		
		Assert.notNull(webScaffoldAnnotationValues.getPath(), "Path is not specified in the @RooWebScaffold annotation for '" + webScaffoldAnnotationValues.getGovernorTypeDetails().getName() + "'");
		
		if (webScaffoldAnnotationValues.getPath().startsWith("/")) {
			controllerPath = webScaffoldAnnotationValues.getPath().substring(1);
		} else {
			controllerPath = webScaffoldAnnotationValues.getPath();
		}	
	}
	
	public Document getListDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();
		
		document.createComment(warning);
		
		//add document namespaces
		Element div = (Element) document.appendChild(new XmlElementBuilder("div", document)
								.addAttribute("xmlns:roo", "urn:jsptagdir:/WEB-INF/tags")
								.addAttribute("xmlns:form", "http://www.springframework.org/tags/form")
								.addAttribute("xmlns:spring", "http://www.springframework.org/tags")
								.addAttribute("xmlns:c", "http://java.sun.com/jsp/jstl/core")
								.addAttribute("xmlns:fmt", "http://java.sun.com/jsp/jstl/fmt")
								.addAttribute("xmlns:fn", "http://java.sun.com/jsp/jstl/functions")
								.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
								.addAttribute("version", "2.0")
								.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
							.build());
		
//		div.appendChild(DojoUtils.getTitlePaneDojo(document, "${title_msg}"));

		Element divElement = new XmlElementBuilder("div", document).addAttribute("id", "_title_div")
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "label." + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase()).addAttribute("var", "entity_label").build())
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "label." + entityMetadata.getPlural().toLowerCase()).addAttribute("var", "entity_label_plural").build())
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "entity.list.all").addAttribute("arguments", "${entity_label_plural}").addAttribute("var", "title_msg").build())
									.addChild(DojoUtils.getTitlePaneDojo(document, "${title_msg}"))
								.build();
		
		Element ifElement = document.createElementNS("http://java.sun.com/jsp/jstl/core", "c:if");
		ifElement.setAttribute("test", "${not empty " + entityMetadata.getPlural().toLowerCase() + "}");
		Element tableElement = document.createElement("table");
		tableElement.setAttribute("width", "300px");
		ifElement.appendChild(tableElement);
		Element theadElement = document.createElement("thead");
		tableElement.appendChild(theadElement);
		Element trElement = document.createElement("tr");
		theadElement.appendChild(trElement);		

		Element idThElement = document.createElement("th");
		idThElement.setTextContent(entityMetadata.getIdentifierField().getFieldName().getReadableSymbolName());
		trElement.appendChild(idThElement);
		
		int fieldCounter = 0;
		for (FieldMetadata field : fields) {
			Element thElement = document.createElement("th");
			thElement.setTextContent(field.getFieldName().getReadableSymbolName());
			if(++fieldCounter < 7) {
				trElement.appendChild(thElement);
			}
		}
		trElement.appendChild(document.createElement("th"));
		trElement.appendChild(document.createElement("th"));
		trElement.appendChild(document.createElement("th"));

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
				tdElement.setTextContent("${fn:length(" + entityName + "." + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + ")}");
			} else if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				Element fmt = document.createElement("fmt:formatDate");
				fmt.setAttribute("value", "${" + entityName + "." + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + "}");
				fmt.setAttribute("type", "DATE");
				fmt.setAttribute("pattern", dateFormatLocalized.toPattern());
				tdElement.appendChild(fmt);
			} else {
				tdElement.setTextContent("${fn:substring(" + entityName + "." + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + ", 0, 10)}");
			}
			if(++fieldCounter < 7) {
				trElement2.appendChild(tdElement);
			}
		}		
		
		Element showUrl = new XmlElementBuilder("spring:url", document).addAttribute("var", "show_form_url").addAttribute("value", "/" + controllerPath + "/${" + entityName + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}").build();
		Element showImageUrl = new XmlElementBuilder("spring:url", document).addAttribute("var", "show_image_url").addAttribute("value", "/static/images/show.png").build();
		Element showMessage = new XmlElementBuilder("spring:message", document).addAttribute("code", "entity.show").addAttribute("arguments", "${entity_label}").addAttribute("var", "show_label").build();
		Element showSubmitElement = new XmlElementBuilder("input", document).addAttribute("type", "image").addAttribute("class", "image").addAttribute("title", "${show_label}").addAttribute("src", "${show_image_url}").addAttribute("value", "${show_label}").addAttribute("alt", "${show_label}").build();
		Element showFormElement = new XmlElementBuilder("form:form", document).addAttribute("action", "${show_form_url}").addAttribute("method", "GET").addChild(showMessage).addChild(showSubmitElement).build();
		trElement2.appendChild(new XmlElementBuilder("td", document).addChild(showUrl).addChild(showImageUrl).addChild(showFormElement).build());
		
		if(webScaffoldAnnotationValues.isUpdate()) {
			Element updateElement = document.createElement("td");		
			Element updateFormElement = document.createElement("form:form");
			Element updateUrl = document.createElement("spring:url");
			updateUrl.setAttribute("var", "update_form_url");
			updateUrl.setAttribute("value", "/" + controllerPath + "/${" + entityName + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}/form");
			updateElement.appendChild(updateUrl);
			updateFormElement.setAttribute("action", "${update_form_url}");
			updateFormElement.setAttribute("method", "GET");
			Element updateImageUrl = document.createElement("spring:url");
			updateImageUrl.setAttribute("var", "update_image_url");
			updateImageUrl.setAttribute("value", "/static/images/update.png");
			updateElement.appendChild(updateImageUrl);
			Element updateMessage = document.createElement("spring:message");
			updateMessage.setAttribute("code", "entity.update");
			updateMessage.setAttribute("arguments", "${entity_label}");
			updateMessage.setAttribute("var", "update_label");
			updateFormElement.appendChild(updateMessage);
			Element updateSubmitElement = document.createElement("input");
			updateSubmitElement.setAttribute("type", "image");
			updateSubmitElement.setAttribute("class", "image");
			updateSubmitElement.setAttribute("title", "${update_label}");
			updateSubmitElement.setAttribute("src", "${update_image_url}");
			updateSubmitElement.setAttribute("value", "${update_label}");
			updateSubmitElement.setAttribute("alt", "${update_label}");
			updateFormElement.appendChild(updateSubmitElement);
			updateElement.appendChild(updateFormElement);
			trElement2.appendChild(updateElement);
		}

		if(webScaffoldAnnotationValues.isDelete()) {
			Element deleteElement = document.createElement("td");
			Element deleteFormElement = document.createElement("form:form");
			Element deleteUrl = document.createElement("spring:url");
			deleteUrl.setAttribute("var", "delete_form_url");
			deleteUrl.setAttribute("value", "/" + controllerPath + "/${" + entityName + "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}");
			deleteElement.appendChild(deleteUrl);
			deleteFormElement.setAttribute("action", "${delete_form_url}");
			deleteFormElement.setAttribute("method", "DELETE");
			Element deleteImageUrl = document.createElement("spring:url");
			deleteImageUrl.setAttribute("var", "delete_image_url");
			deleteImageUrl.setAttribute("value", "/static/images/delete.png");
			deleteElement.appendChild(deleteImageUrl);
			Element deleteMessage = document.createElement("spring:message");
			deleteMessage.setAttribute("code", "entity.delete");
			deleteMessage.setAttribute("arguments", "${entity_label}");
			deleteMessage.setAttribute("var", "delete_label");
			deleteFormElement.appendChild(deleteMessage);
			deleteFormElement.appendChild(new XmlElementBuilder("input", document).addAttribute("type", "image").addAttribute("class", "image").addAttribute("title", "${delete_label}").addAttribute("src", "${delete_image_url}").addAttribute("value", "${delete_label}").addAttribute("alt", "${delete_label}").build());
			deleteFormElement.appendChild(new XmlElementBuilder("c:if", document).addAttribute("test", "${not empty param.page}")
						.addChild(new XmlElementBuilder("input", document).addAttribute("type", "hidden").addAttribute("value", "${param.page}").addAttribute("name", "page").build())
					.build());
			deleteFormElement.appendChild(new XmlElementBuilder("c:if", document).addAttribute("test", "${not empty param.size}")
						.addChild(new XmlElementBuilder("input", document).addAttribute("type", "hidden").addAttribute("value", "${param.size}").addAttribute("name", "size").build())
					.build());
			deleteElement.appendChild(deleteFormElement);
			trElement2.appendChild(deleteElement);
		}
		
		Element notFoundMessage = document.createElement("spring:message");
		notFoundMessage.setAttribute("code", "entity.not.found");
		notFoundMessage.setAttribute("arguments", "${entity_label_plural}");
		
		Element elseElement = document.createElement("c:if");
		elseElement.setAttribute("test", "${empty " + entityMetadata.getPlural().toLowerCase() + "}");
		elseElement.appendChild(notFoundMessage);	
		divElement.appendChild(ifElement);
		
		Element bottomTd = new XmlElementBuilder("td", document).addAttribute("colspan", "" + (fields.size() > 7 ? 10 : (fields.size() + 4))).build();
		if(webScaffoldAnnotationValues.isCreate()) {
			//create new entity
			bottomTd.appendChild(new XmlElementBuilder("span", document).addAttribute("class", "new")
				.addChild(new XmlElementBuilder("spring:url", document).addAttribute("value", "/" + controllerPath + "/form").addAttribute("var", "create_url").build())
				.addChild(new XmlElementBuilder("a", document).addAttribute("href", "${create_url}")
					.addChild(new XmlElementBuilder("spring:url", document).addAttribute("value", "/static/images/add.png").addAttribute("var", "create_img_url").build())
					.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "global.menu.new").addAttribute("arguments", "${entity_label}").addAttribute("var", "add_message").build())
					.addChild(new XmlElementBuilder("img", document).addAttribute("src", "${create_img_url}").addAttribute("title", "${add_message}").addAttribute("alt", "${add_message}").build())
				.build())
			.build());
		}
		//pagination
		bottomTd.appendChild(new XmlElementBuilder("c:if", document).addAttribute("test", "${not empty maxPages}")
				.addChild(new XmlElementBuilder("roo:pagination", document).addAttribute("maxPages", "${maxPages}").addAttribute("page", "${param.page}").addAttribute("size", "${param.size}").build())
		.build());
		
		tableElement.appendChild(new XmlElementBuilder("tr", document).addAttribute("class", "footer").addChild(bottomTd).build());

		divElement.appendChild(elseElement);
		div.appendChild(divElement);
		
		return document;
	}
	
	public Document getShowDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		
		
		//add document namespaces
		Element div = new XmlElementBuilder("div", document)
							.addAttribute("xmlns:form", "http://www.springframework.org/tags/form")
							.addAttribute("xmlns:spring", "http://www.springframework.org/tags")
							.addAttribute("xmlns:c", "http://java.sun.com/jsp/jstl/core")
							.addAttribute("xmlns:fmt", "http://java.sun.com/jsp/jstl/fmt")
							.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
							.addAttribute("version", "2.0")
							.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
						.build();
		document.appendChild(div);
		
		Element divElement = new XmlElementBuilder("div", document).addAttribute("id", "_title_div")
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "label." + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase()).addAttribute("var", "entity_label").build())
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "entity.show").addAttribute("arguments", "${entity_label}").addAttribute("var", "title_msg").build())
									.addChild(DojoUtils.getTitlePaneDojo(document, "${title_msg}"))
								.build();
		
		Element ifElement = document.createElement("c:if");
		ifElement.setAttribute("test", "${not empty " + entityName + "}");
		for (FieldMetadata field : fields) {
			Element divSubmitElement = document.createElement("div");
			divSubmitElement.setAttribute("id", "roo_" + entityName + "_" + StringUtils.uncapitalize(field.getFieldName().getSymbolName()));
				
			Element label = document.createElement("label");
			label.setAttribute("for", "_" + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + "_id");
			label.setTextContent(field.getFieldName().getReadableSymbolName() + ":");
			divSubmitElement.appendChild(label);
			
			Element divContent = document.createElement("div");
			divContent.setAttribute("id", "_" + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + "_id");
			divContent.setAttribute("class", "box");
			
			if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
				Element fmt = document.createElement("fmt:formatDate");
				fmt.setAttribute("value", "${" + entityName + "." + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + "}");
				fmt.setAttribute("type", "DATE");
				fmt.setAttribute("pattern", dateFormatLocalized.toPattern());
				divContent.appendChild(fmt);
			} else {
				divContent.setTextContent("${" + entityName + "." + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + "}");
			}
			divSubmitElement.appendChild(divContent);
			ifElement.appendChild(divSubmitElement);
			ifElement.appendChild(document.createElement("br"));
		}
		divElement.appendChild(ifElement);
		
		Element notFoundMessage = document.createElement("spring:message");
		notFoundMessage.setAttribute("code", "entity.not.found.single");
		notFoundMessage.setAttribute("arguments", "${entity_label}");
		
		Element elseElement = document.createElement("c:if");
		elseElement.setAttribute("test", "${empty " + entityName + "}");
		elseElement.appendChild(notFoundMessage);
		divElement.appendChild(elseElement);
		div.appendChild(divElement);
		
		return document;
	}
	
	public Document getCreateDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		

		//add document namespaces
		Element div = new XmlElementBuilder("div", document)
							.addAttribute("xmlns:form", "http://www.springframework.org/tags/form")
							.addAttribute("xmlns:spring", "http://www.springframework.org/tags")
							.addAttribute("xmlns:c", "http://java.sun.com/jsp/jstl/core")
							.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
							.addAttribute("version", "2.0")
							.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
						.build();
		document.appendChild(div);

		Element divElement = new XmlElementBuilder("div", document).addAttribute("id", "_title_div")
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "label." + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase()).addAttribute("var", "entity_label").build())
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "entity.create").addAttribute("arguments", "${entity_label}").addAttribute("var", "title_msg").build())
									.addChild(DojoUtils.getTitlePaneDojo(document, "${title_msg}"))
								.build();
		
		Element url = document.createElement("spring:url");
		url.setAttribute("var", "form_url");
		url.setAttribute("value", "/" + controllerPath);
		divElement.appendChild(url);
		
		Element formElement = document.createElement("form:form");
		formElement.setAttribute("modelAttribute", entityName);
		formElement.setAttribute("action", "${form_url}");
		formElement.setAttribute("method", "POST");

		createFieldsForCreateAndUpdate(document, formElement);

		Element divSubmitElement = document.createElement("div");
		divSubmitElement.setAttribute("id", "roo_" + entityName + "_submit");
		divSubmitElement.setAttribute("class", "submit");
		
		Element saveBottonLabel = document.createElement("spring:message");
		saveBottonLabel.setAttribute("code", "button.save");
		saveBottonLabel.setAttribute("var", "save_button");
		divSubmitElement.appendChild(saveBottonLabel);
		
		Element inputElement = document.createElement("input");
		inputElement.setAttribute("type", "submit");
		inputElement.setAttribute("value", "${save_button}");
		inputElement.setAttribute("id", "proceed");
		divSubmitElement.appendChild(DojoUtils.getSubmitButtonDojo(document, "proceed"));
		divSubmitElement.appendChild(inputElement);
		formElement.appendChild(divSubmitElement);

		divElement.appendChild(formElement);
		div.appendChild(divElement);	
		return document;
	}
	
	public Document getUpdateDocument() {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		

		//add document namespaces
		Element div = new XmlElementBuilder("div", document)
							.addAttribute("xmlns:form", "http://www.springframework.org/tags/form")
							.addAttribute("xmlns:spring", "http://www.springframework.org/tags")
							.addAttribute("xmlns:c", "http://java.sun.com/jsp/jstl/core")
							.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
							.addAttribute("version", "2.0")
							.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
						.build();
		document.appendChild(div);
		
		Element divElement = new XmlElementBuilder("div", document).addAttribute("id", "_title_div")
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "label." + beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase()).addAttribute("var", "entity_label").build())
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "entity.update").addAttribute("arguments", "${entity_label}").addAttribute("var", "title_msg").build())
									.addChild(DojoUtils.getTitlePaneDojo(document, "${title_msg}"))
								.build();
		
		Element url = document.createElement("spring:url");
		url.setAttribute("var", "form_url");
		url.setAttribute("value", "/" + controllerPath + "/${" + entityName	+ "." + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "}");
		divElement.appendChild(url);
		
		Element formElement = document.createElement("form:form");
		formElement.setAttribute("modelAttribute", entityName);
		formElement.setAttribute("action", "${form_url}");
		formElement.setAttribute("method", "PUT");		

		createFieldsForCreateAndUpdate(document, formElement);
		
		Element divSubmitElement = document.createElement("div");
		divSubmitElement.setAttribute("id", "roo_" + entityName + "_submit");
		divSubmitElement.setAttribute("class", "submit");
		
		Element updateBottonLabel = document.createElement("spring:message");
		updateBottonLabel.setAttribute("code", "button.update");
		updateBottonLabel.setAttribute("var", "update_button");
		divSubmitElement.appendChild(updateBottonLabel);
		
		Element inputElement = document.createElement("input");
		inputElement.setAttribute("type", "submit");
		inputElement.setAttribute("value", "${update_button}");
		inputElement.setAttribute("id", "proceed");
		divSubmitElement.appendChild(DojoUtils.getSubmitButtonDojo(document, "proceed"));
		divSubmitElement.appendChild(inputElement);
		formElement.appendChild(divSubmitElement);
		
		Element formHiddenId = document.createElement("form:hidden");
		formHiddenId.setAttribute("path", entityMetadata.getIdentifierField().getFieldName().getSymbolName());
		formHiddenId.setAttribute("id", "_" + entityMetadata.getIdentifierField().getFieldName().getSymbolName() + "_id");
		formElement.appendChild(formHiddenId);
		if (null != entityMetadata.getVersionField()) {
			Element formHiddenVersion = document.createElement("form:hidden");
			formHiddenVersion.setAttribute("path", entityMetadata.getVersionField().getFieldName().getSymbolName());
			formHiddenVersion.setAttribute("id", "_" + entityMetadata.getVersionField().getFieldName().getSymbolName() + "_id");
			formElement.appendChild(formHiddenVersion);
		}

		divElement.appendChild(formElement);
		div.appendChild(divElement);
	
		return document;
	}
	
	public Document getFinderDocument(String finderName) {
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();
		Document document = builder.newDocument();		
		
		//add document namespaces
		Element div = new XmlElementBuilder("div", document)
							.addAttribute("xmlns:form", "http://www.springframework.org/tags/form")
							.addAttribute("xmlns:spring", "http://www.springframework.org/tags")
							.addAttribute("xmlns:c", "http://java.sun.com/jsp/jstl/core")
							.addAttribute("xmlns:jsp", "http://java.sun.com/JSP/Page")
							.addAttribute("version", "2.0")
							.addChild(new XmlElementBuilder("jsp:output", document).addAttribute("omit-xml-declaration", "yes").build())
						.build();
		document.appendChild(div);
		
		Element titleDivElement = new XmlElementBuilder("div", document).addAttribute("id", "_title_div")
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "label." + finderName.replace("find" + entityMetadata.getPlural() + "By", "").toLowerCase()).addAttribute("var", "entity_label").build())
									.addChild(new XmlElementBuilder("spring:message", document).addAttribute("code", "entity.find").addAttribute("arguments", "${entity_label}").addAttribute("var", "title_msg").build())
									.addChild(DojoUtils.getTitlePaneDojo(document, "${title_msg}"))
								.build();
		
		Element url = document.createElement("spring:url");
		url.setAttribute("var", "form_url");
		url.setAttribute("value", "/" + controllerPath + "/find/" + finderName.replace("find" + entityMetadata.getPlural(), ""));
		titleDivElement.appendChild(url);
		
		Element formElement = document.createElement("form:form");
		formElement.setAttribute("action", "${form_url}");
		formElement.setAttribute("method", "GET");		

		MethodMetadata methodMetadata = finderMetadata.getDynamicFinderMethod(finderName, beanInfoMetadata.getJavaBean().getSimpleTypeName().toLowerCase());
		
		List<JavaType> types = AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodMetadata.getParameterTypes());
		List<JavaSymbolName> paramNames = methodMetadata.getParameterNames();
		
		for (int i = 0; i < types.size(); i++) {
			
			JavaType type = types.get(i);
			JavaSymbolName paramName = paramNames.get(i);
			
			Element divElement = document.createElement("div");
			divElement.setAttribute("id", "roo_" + entityName + "_" + paramName.getSymbolName().toLowerCase());

			Element labelElement = document.createElement("label");
			labelElement.setAttribute("for", "_" + paramName.getSymbolName().toLowerCase() + "_id");
			labelElement.setTextContent(paramName.getReadableSymbolName()  + ":");
			
			if (type.isCommonCollectionType() && isSpecialType(type.getParameters().get(0))) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type.getParameters().get(0), Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					Element ifElement = document.createElement("c:if");
					ifElement.setAttribute("test", "${not empty " + typeEntityMetadata.getPlural().toLowerCase() + "}");
					divElement.appendChild(ifElement);
					ifElement.appendChild(labelElement);

					Element select = document.createElement("select");
					select.setAttribute("style", "width:250px");
					select.setAttribute("name", paramName.getSymbolName().toLowerCase());
					select.setAttribute("id", "_" + paramName.getSymbolName().toLowerCase() + "_id");
					Element forEach = document.createElement("c:forEach");
					forEach.setAttribute("items", "${" + typeEntityMetadata.getPlural().toLowerCase() + "}");
					forEach.setAttribute("var", paramName.getSymbolName().toLowerCase() + "_item");
					select.appendChild(forEach);
					Element option = document.createElement("option");
					option.setAttribute("value", "${" + paramName.getSymbolName().toLowerCase() + "_item." + entityMetadata.getIdentifierField().getFieldName() + "}");
					option.setTextContent("${" + paramName.getSymbolName().toLowerCase()  + "_item}");
					forEach.appendChild(option);
					ifElement.appendChild(select);
					ifElement.appendChild(DojoUtils.getMultiSelectDojo(document, new JavaSymbolName(paramName.getSymbolName())));
				}
			} else if (isSpecialType(type)) {
				EntityMetadata typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(type, Path.SRC_MAIN_JAVA));
				if (typeEntityMetadata != null) {
					Element ifElement = document.createElement("c:if");
					ifElement.setAttribute("test", "${not empty " + typeEntityMetadata.getPlural().toLowerCase() + "}");
					divElement.appendChild(ifElement);
					ifElement.appendChild(labelElement);

					Element select = document.createElement("select");
					select.setAttribute("style", "width:250px");
					select.setAttribute("name", paramName.getSymbolName().toLowerCase());
					Element forEach = document.createElement("c:forEach");
					forEach.setAttribute("items", "${" + typeEntityMetadata.getPlural().toLowerCase() + "}");
					forEach.setAttribute("var", paramName.getSymbolName().toLowerCase() + "_item");
					select.appendChild(forEach);
					Element option = document.createElement("option");
					option.setAttribute("value", "${" + paramName.getSymbolName().toLowerCase() + "_item." + entityMetadata.getIdentifierField().getFieldName() + "}");
					option.setTextContent("${" + paramName.getSymbolName().toLowerCase()  + "_item}");
					forEach.appendChild(option);					
					ifElement.appendChild(select);
					ifElement.appendChild(DojoUtils.getSelectDojo(document, new JavaSymbolName(paramName.getSymbolName())));
				}
			} else if (isEnumType(type)) {
				divElement.appendChild(labelElement);
				divElement.appendChild(JspUtils.getEnumSelectBox(document, type, paramName));		
				divElement.appendChild(DojoUtils.getSelectDojo(document, paramName));
				divElement.appendChild(document.createElement("br"));
				formElement.appendChild(divElement);
				formElement.appendChild(document.createElement("br"));	
			} else if (type.getFullyQualifiedTypeName().equals(Boolean.class.getName())
					|| type.getFullyQualifiedTypeName().equals(boolean.class.getName())) {	
				divElement.appendChild(labelElement);
				Element formCheckTrue = document.createElement("input");
				formCheckTrue.setAttribute("type", "radio");
				formCheckTrue.setAttribute("id", "_" + paramName.getSymbolName() + "_id");
				formCheckTrue.setAttribute("name", paramName.getSymbolName().toLowerCase());
				formCheckTrue.setAttribute("value", "true");
				formCheckTrue.setAttribute("checked", "checked");
				formCheckTrue.setTextContent("(true)");
				divElement.appendChild(formCheckTrue);
				Element formCheckFalse = (Element)formCheckTrue.cloneNode(false);
				formCheckFalse.setAttribute("value", "false");
				formCheckFalse.setTextContent("(false)");
				formCheckFalse.removeAttribute("checked");
				divElement.appendChild(formCheckFalse);
				formElement.appendChild(divElement);
				formElement.appendChild(document.createElement("br"));
			} else {	
				divElement.appendChild(labelElement);
				Element formInput = document.createElement("input");
				formInput.setAttribute("name", paramName.getSymbolName().toLowerCase());
				formInput.setAttribute("id", "_" + paramName.getSymbolName().toLowerCase() + "_id");
				formInput.setAttribute("size", "0");
				formInput.setAttribute("style", "width:250px");
				divElement.appendChild(formInput);
				Element required = document.createElement("spring:message");
				required.setAttribute("code", "field.required");
				required.setAttribute("var", "field_required");
				divElement.appendChild(required);
				Element dojoMessage = document.createElement("spring:message");
				dojoMessage.setAttribute("code", "field.simple.validation");
				dojoMessage.setAttribute("arguments", paramName.getReadableSymbolName() + ",(${field_required})");
				dojoMessage.setAttribute("argumentSeparator", ",");
				dojoMessage.setAttribute("var", "validation_required");
				divElement.appendChild(dojoMessage);				
				divElement.appendChild(DojoUtils.getSimpleValidationDojo(document, paramName));
				
				if (type.getFullyQualifiedTypeName().equals(Date.class.getName()) ||
						// should be tested with instanceof
						type.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
							divElement.appendChild(DojoUtils.getRequiredDateDojo(document, paramName, dateFormatLocalized));
				}
			}
			
			formElement.appendChild(divElement);
			formElement.appendChild(document.createElement("br"));
		}		
		
		Element divSubmitElement = document.createElement("div");
		divSubmitElement.setAttribute("id", "roo_" + entityName + "_submit");
		divSubmitElement.setAttribute("class", "submit");
		
		Element updateBottonLabel = document.createElement("spring:message");
		updateBottonLabel.setAttribute("code", "button.find");
		updateBottonLabel.setAttribute("var", "find_button");
		divSubmitElement.appendChild(updateBottonLabel);
		
		Element inputElement = document.createElement("input");
		inputElement.setAttribute("type", "submit");
		inputElement.setAttribute("value", "${find_button}");
		inputElement.setAttribute("id", "proceed");
		divSubmitElement.appendChild(DojoUtils.getSubmitButtonDojo(document, "proceed"));
		divSubmitElement.appendChild(inputElement);
		formElement.appendChild(divSubmitElement);

		titleDivElement.appendChild(formElement);
		div.appendChild(titleDivElement);
	
		return document;
	}	
	
	private void createFieldsForCreateAndUpdate(Document document, Element formElement) {
		
		formElement.appendChild(new XmlElementBuilder("form:errors", document).addAttribute("delimiter", "<p/>").addAttribute("cssClass", "errors").build());
		
		for (FieldMetadata field : fields) {
			JavaType fieldType = field.getFieldType();
			if(fieldType.isCommonCollectionType() && fieldType.equals(new JavaType(Set.class.getName()))) {
				if (fieldType.getParameters().size() != 1) {
					throw new IllegalArgumentException("A set is defined without specification of its type (via generics) - unable to create view for it");
				}
				fieldType = fieldType.getParameters().get(0);
			}
			
			Element divElement = document.createElement("div");
			divElement.setAttribute("id", "roo_" + entityName + "_" + StringUtils.uncapitalize(field.getFieldName().getSymbolName()));
						
			Element labelElement = document.createElement("label");
			labelElement.setAttribute("for", "_" + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + "_id");
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
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToMany")) {
						//OneToMany relationships are not managed from the one side, therefore we skip this field
						return;
					}
					if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToMany")
							|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToOne")) {

						EntityMetadata typeEntityMetadata = null;
						
						if (field.getFieldType().isCommonCollectionType()) {
							
							//currently there is no scaffolding available for Maps (see ROO-194)
							if(field.getFieldType().equals(new JavaType(Map.class.getName()))) {
								formElement.appendChild(divElement);
								formElement.appendChild(document.createElement("br"));
								return;
							}
							
							typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(field.getFieldType().getParameters().get(0), Path.SRC_MAIN_JAVA));
						} else {
							typeEntityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(field.getFieldType(), Path.SRC_MAIN_JAVA));
						}
	
						if(typeEntityMetadata == null) {
							throw new IllegalStateException("Could not determine the plural name for the '" + StringUtils.uncapitalize(field.getFieldName().getSymbolName()) + "' field in " + beanInfoMetadata.getJavaBean().getSimpleTypeName());
						}
						
						String plural = typeEntityMetadata.getPlural().toLowerCase();
						
						Element ifElement = document.createElement("c:if");
						ifElement.setAttribute("test", "${not empty " + plural + "}");
						divElement.appendChild(ifElement);
						
						divElement.removeChild(labelElement);
						ifElement.appendChild(labelElement);
						
						ifElement.appendChild(JspUtils.getSelectBox(document, field.getFieldName(), plural, entityMetadata.getIdentifierField()));		

						specialAnnotation = true;
						formElement.appendChild(divElement);
						formElement.appendChild(document.createElement("br"));
						
						if(annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.ManyToOne")
								|| annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.OneToOne")) {
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
								String fieldCode = "field.invalid";
								if(field.getFieldType().equals(new JavaType(Integer.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(int.class.getName())) {
									fieldCode = "field.invalid.integer";
								} else if (StringUtils.uncapitalize(field.getFieldName().getSymbolName()).contains("email")) {
									fieldCode = "field.invalid.email";
								} else if(field.getFieldType().equals(new JavaType(Double.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(double.class.getName())
										|| field.getFieldType().equals(new JavaType(Float.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(float.class.getName())) {
									fieldCode = "field.invalid.number";
								}
								
								Element invalidMessage = document.createElement("spring:message");
								invalidMessage.setAttribute("code", fieldCode);
								if ("field.invalid".equals(fieldCode))
									invalidMessage.setAttribute("arguments", field.getFieldName().getReadableSymbolName());
								invalidMessage.setAttribute("var", "field_invalid");
								divElement.appendChild(invalidMessage);
								
								Element required = document.createElement("spring:message");
								required.setAttribute("code", "field.required");
								required.setAttribute("var", "field_required");
								divElement.appendChild(required);
								
								Element validMessage = document.createElement("spring:message");
								validMessage.setAttribute("code", "field.simple.validation");
								validMessage.setAttribute("arguments", field.getFieldName().getReadableSymbolName() + (isTypeInAnnotationList(new JavaType("javax.validation.constraints.NotNull"), field.getAnnotations()) ? ",(${field_required})" : ","));
								validMessage.setAttribute("argumentSeparator", ",");
								validMessage.setAttribute("var", "field_validation");
								divElement.appendChild(validMessage);
								divElement.appendChild(DojoUtils.getValidationDojo(document, field));
							}							
							formElement.appendChild(divElement);
							formElement.appendChild(document.createElement("br"));	
							specialAnnotation = true;
						}
					} else if (annotation.getAnnotationType().getFullyQualifiedTypeName().equals("javax.persistence.Enumerated") && isEnumType(field.getFieldType())) {
						divElement.appendChild(JspUtils.getErrorsElement(document, field.getFieldName()));
						divElement.appendChild(JspUtils.getEnumSelectBox(document, fieldType, field.getFieldName()));		
						divElement.appendChild(document.createElement("br"));
						divElement.appendChild(DojoUtils.getSelectDojo(document, field.getFieldName()));
						formElement.appendChild(divElement);
						formElement.appendChild(document.createElement("br"));	
						specialAnnotation = true;
					}
				}
				if (!specialAnnotation) {
					divElement.appendChild(JspUtils.getInputBox(document, field.getFieldName(), 30));
					divElement.appendChild(document.createElement("br"));
					divElement.appendChild(JspUtils.getErrorsElement(document, field.getFieldName()));
					String fieldCode = "field.invalid";
					if(field.getFieldType().equals(new JavaType(Integer.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(int.class.getName())) {
						fieldCode = "field.invalid.integer";
					} else if (StringUtils.uncapitalize(field.getFieldName().getSymbolName()).contains("email")) {
						fieldCode = "field.invalid.email";
					} else if(field.getFieldType().equals(new JavaType(Double.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(double.class.getName())
							|| field.getFieldType().equals(new JavaType(Float.class.getName())) || field.getFieldType().getFullyQualifiedTypeName().equals(float.class.getName())) {
						fieldCode = "field.invalid.number";
					}
					Element invalidMessage = document.createElement("spring:message");
					invalidMessage.setAttribute("code", fieldCode);
					if ("field.invalid".equals(fieldCode))
						invalidMessage.setAttribute("arguments", field.getFieldName().getReadableSymbolName());
					invalidMessage.setAttribute("var", "field_invalid");
					divElement.appendChild(invalidMessage);
					
					Element required = document.createElement("spring:message");
					required.setAttribute("code", "field.required");
					required.setAttribute("var", "field_required");
					divElement.appendChild(required);
					
					Element validMessage = document.createElement("spring:message");
					validMessage.setAttribute("code", "field.simple.validation");
					validMessage.setAttribute("arguments", field.getFieldName().getReadableSymbolName() + (isTypeInAnnotationList(new JavaType("javax.validation.constraints.NotNull"), field.getAnnotations()) ? ",(${field_required})" : ","));
					validMessage.setAttribute("argumentSeparator", ",");
					validMessage.setAttribute("var", "field_validation");
					
					divElement.appendChild(validMessage);
					divElement.appendChild(DojoUtils.getValidationDojo(document, field));
					
					if (fieldType.getFullyQualifiedTypeName().equals(Date.class.getName()) ||
							// should be tested with instanceof
									fieldType.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {
								divElement.appendChild(DojoUtils.getDateDojo(document, field, dateFormatLocalized));
					}

					formElement.appendChild(divElement);
					formElement.appendChild(document.createElement("br"));				
				}
			}
		}
	}
	
	private static boolean isTypeInAnnotationList(JavaType type, List<AnnotationMetadata> annotations) {
		for (AnnotationMetadata annotation : annotations) {
			if(annotation.getAnnotationType().equals(type)) {
				return true;
			}
		}
		return false;
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
