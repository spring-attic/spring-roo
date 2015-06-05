package org.springframework.roo.web.ui.domain;
/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class Database {

    private String databaseName;
    
    public Database(String fDatabaseName){
    	this.setDatabaseName(fDatabaseName);
    }

	/**
	 * @return the databaseName
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @param databaseName the databaseName to set
	 */
	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	
    
	

}