package org.springframework.roo.web.ui.domain;
/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class PersistenceProvider {

    private String providerName;
    
    public PersistenceProvider(String fProviderName){
    	this.setProviderName(fProviderName);
    }

	/**
	 * @return the providerName
	 */
	public String getProviderName() {
		return providerName;
	}

	/**
	 * @param providerName the providerName to set
	 */
	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	
    
	

}