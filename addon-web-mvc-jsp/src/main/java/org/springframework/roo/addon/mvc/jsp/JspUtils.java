package org.springframework.roo.addon.mvc.jsp;

import org.springframework.roo.model.JavaSymbolName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JspUtils {

	public static Element getInputBox(Document document, JavaSymbolName field, Integer maxValue) {
		Element formInput = document.createElement("form:input");
		formInput.setAttribute("path", field.getSymbolName());
		formInput.setAttribute("id", "_" + field.getSymbolName());
		formInput.setAttribute("size", "0");
		formInput.setAttribute("cssStyle", "width:250px");
		formInput.setAttribute("maxlength", maxValue.toString());
		return formInput;
	}
	
	public static Element getSelectBox(Document document, JavaSymbolName fieldName, String pluralName) {
		Element formSelect = document.createElement("form:select");
		formSelect.setAttribute("path", fieldName.getSymbolName());
		formSelect.setAttribute("cssStyle", "width:250px");						
		formSelect.setAttribute("id", "_" + fieldName);
		Element formOptions = document.createElement("form:options");
		formOptions.setAttribute("items", "${" + pluralName.toLowerCase() + "}");
		formOptions.setAttribute("itemValue", "id");	
		formSelect.appendChild(formOptions);		
		return formSelect;
	}
	
	public static Element getEnumSelectBox(Document document, JavaSymbolName fieldName) {
		Element formSelect = document.createElement("form:select");
		formSelect.setAttribute("path", fieldName.getSymbolName());
		formSelect.setAttribute("cssStyle", "width:250px");						
		formSelect.setAttribute("id", "_" + fieldName.getSymbolName());
		formSelect.setAttribute("items", "${_" + fieldName.getSymbolName() + "}");		
		return formSelect;
	}
	
	public static Element getTextArea(Document document, JavaSymbolName fieldName, Integer maxValue) {
		Element textArea = document.createElement("form:textarea");
		textArea.setAttribute("path", fieldName.getSymbolName());
		textArea.setAttribute("id", "_" + fieldName.getSymbolName());
		textArea.setAttribute("cssStyle", "width:250px");
		return textArea;
	}
	
	public static Element getCheckBox(Document document, JavaSymbolName fieldName) {
		Element formCheck = document.createElement("form:checkbox");
		formCheck.setAttribute("path", fieldName.getSymbolName());
		formCheck.setAttribute("id", "_" + fieldName.getSymbolName());
		return formCheck;
	}
	
	public static Element getErrorsElement(Document document, JavaSymbolName field) {
		Element errors = document.createElement("form:errors");
		errors.setAttribute("path", field.getSymbolName());
		errors.setAttribute("id", "_" + field.getSymbolName());
		errors.setAttribute("cssClass", "errors");
		return errors;
	}
}
