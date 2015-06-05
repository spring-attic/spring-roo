package org.springframework.roo.web.ui.domain;

import java.util.List;

/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class Entity {

    private String entityName;
    private List<String> extendsTypes;
    private boolean isAbstract;
    private List<Field> fields;
    
    public Entity(String fEntityName, List<String> fExtendsTypes, boolean fIsAbstract, List<Field> fFields){
    	this.setEntityName(fEntityName);
    	this.setExtendsTypes(fExtendsTypes);
    	this.setAbstract(fIsAbstract);
    	this.setFields(fFields);
    }

	/**
	 * @return the entityName
	 */
	public String getEntityName() {
		return entityName;
	}

	/**
	 * @param entityName the entityName to set
	 */
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	/**
	 * @return the fields
	 */
	public List<Field> getFields() {
		return fields;
	}

	/**
	 * @param fields the fields to set
	 */
	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	/**
	 * @return the isAbstract
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * @param isAbstract the isAbstract to set
	 */
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	/**
	 * @return the extendsTypes
	 */
	public List<String> getExtendsTypes() {
		return extendsTypes;
	}

	/**
	 * @param extendsTypes the extendsTypes to set
	 */
	public void setExtendsTypes(List<String> extendsTypes) {
		this.extendsTypes = extendsTypes;
	}


	
    
	

}