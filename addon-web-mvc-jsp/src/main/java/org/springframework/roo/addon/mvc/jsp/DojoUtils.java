package org.springframework.roo.addon.mvc.jsp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DojoUtils {
	
	public static Element getTitlePaneDojo(Document document, String title) {
		addDojoDepenency(document, "dijit.TitlePane");		
		Element divElement = document.createElement("div");
		divElement.setAttribute("dojoType", "dijit.TitlePane");
		divElement.setAttribute("title", title);
		divElement.setAttribute("style", "width: 100%");
		return divElement;
	}
	
	public static Element getRequiredDateDojo(Document document, JavaSymbolName fieldName) {	
		SimpleDateFormat dateFormatLocalized = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
		addDojoDepenency(document, "dijit.form.DateTextBox");		
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + fieldName.getSymbolName().toLowerCase()
				+ "\", widgetType : \"dijit.form.DateTextBox\", widgetAttrs : {datePattern : \"" + dateFormatLocalized.toLocalizedPattern() + "\", required : true}})); ");
		return script;
	} 
	
	public static Element getDateDojo(Document document, FieldMetadata field) {
		SimpleDateFormat dateFormatLocalized = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
		addDojoDepenency(document, "dijit.form.DateTextBox");	
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + field.getFieldName().getSymbolName()
				+ "\", widgetType : \"dijit.form.DateTextBox\", widgetAttrs : {datePattern : \"" + dateFormatLocalized.toLocalizedPattern() + "\", required : "
				+ (isTypeInAnnotationList(new JavaType("javax.validation.NotNull"), field.getAnnotations()) ? "true" : "false") + "}})); ");
		return script;
	}
	
	public static Element getSimpleValidationDojo(Document document, JavaSymbolName field) {
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");
		String message = "Enter " + field.getReadableSymbolName() + " (required)";
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + field.getSymbolName().toLowerCase()
				+ "\", widgetType : \"dijit.form.ValidationTextBox\", widgetAttrs : {promptMessage: \"" + message + "\", required : true}})); ");
		return script;
	}

	public static Element getValidationDojo(Document document, FieldMetadata field) {
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
	
	public static Element getTextAreaDojo(Document document, JavaSymbolName fieldName) {
		addDojoDepenency(document, "dijit.form.Textarea");	
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + fieldName.getSymbolName()
				+ "\", widgetType: \"dijit.form.Textarea\"})); ");
		return script;
	}
	
	public static Element getSelectDojo(Document document, JavaSymbolName fieldName) {
		addDojoDepenency(document, "dijit.form.FilteringSelect");
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + fieldName.getSymbolName()
				+ "\", widgetType: \"dijit.form.FilteringSelect\", widgetAttrs : {hasDownArrow : true}})); ");
		return script;
	}
	
	public static Element getMultiSelectDojo(Document document, JavaSymbolName fieldName) {
		addDojoDepenency(document, "dijit.form.MultiSelect");
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ElementDecoration({elementId : \"_" + fieldName.getSymbolName()
				+ "\", widgetType: \"dijit.form.MultiSelect\"})); ");
		return script;
	}
	
	public static Element getSubmitButtonDojo(Document document, String buttonId) {
		Element script = document.createElement("script");
		script.setAttribute("type", "text/javascript");		
		script.setTextContent("Spring.addDecoration(new Spring.ValidateAllDecoration({elementId:'" + buttonId + "', event:'onclick'}));");
		return script;
	}
	
	public static void addDojoDepenency(Document document, String require) {
		
		boolean elementFound = true;
		
		Element script = XmlUtils.findFirstElement("//script[starts-with(text(),'dojo.require')]", document.getDocumentElement());
		
		if(script == null) {
			elementFound = false;
			script = document.createElement("script");
			script.setAttribute("type", "text/javascript");
		}
		
		if (!script.getTextContent().contains(require)) {
			script.setTextContent(script.getTextContent() + "dojo.require(\"" + require + "\");");
		}
		
		if (!elementFound) {			
			document.getDocumentElement().insertBefore(script, XmlUtils.findFirstElement("//directive.include[2]", document.getDocumentElement()).getNextSibling());
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
	
	private static int getMinSize(FieldMetadata field) {		
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
	
	private static int getMaxSize(FieldMetadata field) {	
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
}
