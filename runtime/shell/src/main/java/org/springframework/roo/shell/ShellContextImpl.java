package org.springframework.roo.shell;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides an implementation for {@link ShellContext}
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ShellContextImpl implements ShellContext {

    private boolean force;
    private String executedCommand;
    private Map<String, String> parameters;
    
    public ShellContextImpl(){
        this.force = false;
        this.executedCommand = "";
        this.parameters = new HashMap<String, String>();
    }

    @Override
    public boolean isForce() {
        return this.force;
    }
    
    @Override
    public String getExecutedCommand() {
        return this.executedCommand;
    }

    @Override
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    /**
     * Set current value of --force parameter defined on Spring Roo Shell
     * 
     * @param force
     */
    public void setForce(boolean force) {
        this.force = force;
    }
    
    public void setExecutedCommand(String command){
        this.executedCommand = command;
    }

    /**
     * Set a list of parameters like defined parameters on Spring Roo Shell
     * 
     * @param parameters
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * Add new parameter on current parameters map.
     * 
     * @param key
     * @param value
     */
    public void setParameter(String key, String value) {
        this.parameters.put(key, value);
    }
}
