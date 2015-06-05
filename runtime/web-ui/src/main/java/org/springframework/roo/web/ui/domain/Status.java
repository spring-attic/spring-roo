package org.springframework.roo.web.ui.domain;


/**
 * 
 * @author Juan Carlos García
 * @since 2.0
 *
 */
public class Status {

    private boolean success;
    private String message;
    
    public Status(boolean fSuccess, String fMessage){
    	this.success = fSuccess;
    	this.message = fMessage;
    }
    
	/**
	 * @return the success
	 */
	public boolean isSuccess() {
		return success;
	}
	/**
	 * @param success the success to set
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

}