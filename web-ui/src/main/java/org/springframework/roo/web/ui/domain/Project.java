package org.springframework.roo.web.ui.domain;
/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class Project {

    private String projectName;
    private String topLevelPackage;
    private boolean exists; 
    
    public Project(String fProjectName, String fTopLevelPackage, boolean exists){
    	this.setProjectName(fProjectName);
    	this.setTopLevelPackage(fTopLevelPackage);
    	this.setExists(exists);
    }

	/**
	 * @return the projectName
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * @param projectName the projectName to set
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	/**
	 * @return the topLevelPackage
	 */
	public String getTopLevelPackage() {
		return topLevelPackage;
	}

	/**
	 * @param topLevelPackage the topLevelPackage to set
	 */
	public void setTopLevelPackage(String topLevelPackage) {
		this.topLevelPackage = topLevelPackage;
	}

	/**
	 * @return the exists
	 */
	public boolean isExists() {
		return exists;
	}

	/**
	 * @param exists the exists to set
	 */
	public void setExists(boolean exists) {
		this.exists = exists;
	}
    
	

}