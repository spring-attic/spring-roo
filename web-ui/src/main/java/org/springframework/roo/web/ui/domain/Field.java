package org.springframework.roo.web.ui.domain;


/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class Field {

    private String fieldName;
    private String type;
    private String referencedClass;
    
    public Field(String fFieldName, String fType, String fReferencedClass){
    	this.setFieldName(fFieldName);
    	this.setType(fType);
    	this.setReferencedClass(fReferencedClass);
    }

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * @param fieldName the fieldName to set
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the referencedClass
	 */
	public String getReferencedClass() {
		return referencedClass;
	}

	/**
	 * @param referencedClass the referencedClass to set
	 */
	public void setReferencedClass(String referencedClass) {
		this.referencedClass = referencedClass;
	}



	
    
	

}