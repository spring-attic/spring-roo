package org.springframework.roo.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Simplified immutable representation of a dependency.
 * 
 * <p>
 * Structured after the model used by Maven and Ivy. This may be replaced in a future
 * release with a more OSGi-centric model.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class Dependency implements Comparable<Dependency> {
	private JavaPackage groupId;
	private JavaSymbolName artifactId;
	private String versionId;
	private DependencyType type;
	private List<Dependency> exclusions = new ArrayList<Dependency>();
	
	/**
	 * Convenience constructor for producing a JAR dependency.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param versionId the version ID (required)
	 */
	public Dependency(String groupId, String artifactId, String versionId) {
		Assert.hasText(groupId, "Group ID required");
		Assert.hasText(artifactId, "Artifact ID required");
		Assert.hasText(versionId, "Version ID required");
		this.groupId = new JavaPackage(groupId);
		this.artifactId = new JavaSymbolName(artifactId);
		this.versionId = versionId;
		this.type = DependencyType.JAR;
	}
	

	/**
	 * Convenience constructor for producing a JAR dependency.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param versionId the version ID (required)
	 * @param exclusions the exclusions for this dependency
	 */
	public Dependency(String groupId, String artifactId, String versionId, List<Dependency> exclusions) {
		Assert.hasText(groupId, "Group ID required");
		Assert.hasText(artifactId, "Artifact ID required");
		Assert.hasText(versionId, "Version ID required");
		Assert.notNull(exclusions, "Exclusions required");
		this.groupId = new JavaPackage(groupId);
		this.artifactId = new JavaSymbolName(artifactId);
		this.versionId = versionId;
		this.type = DependencyType.JAR;
		this.exclusions = exclusions;
	}
	
	/**
	 * Convenience constructor when an XML element is available that represents a Maven <dependency>.
	 * 
	 * @param dependency to parse (required)
	 */
	public Dependency(Element dependency) {
		this.type = DependencyType.JAR;
        if (XmlUtils.findFirstElement("/type", dependency) != null || dependency.hasAttribute("type")) {
        	String t;
        	if (dependency.hasAttribute("type")) {
        		t = dependency.getAttribute("type");
        	} else {
        		t = XmlUtils.findFirstElement("//type", dependency).getTextContent().trim().toUpperCase();
        	} 
			if (t.equals("JAR")) {
				// already a JAR, so no need to reassign
			} else if (t.equals("ZIP")) {
			    this.type = DependencyType.ZIP;
			} else {
			    this.type = DependencyType.OTHER;
			}
        }
        //test if it has Maven format
        if (dependency.hasChildNodes() && dependency.getElementsByTagName("artifactId").getLength() > 0) {
        	
        	this.groupId = new JavaPackage("org.apache.maven.plugins");
        	if (dependency.getElementsByTagName("groupId").getLength() > 0) {
            	this.groupId = new JavaPackage(dependency.getElementsByTagName("groupId").item(0).getTextContent());
        	}
        	
	        this.artifactId = new JavaSymbolName(dependency.getElementsByTagName("artifactId").item(0).getTextContent());

	        NodeList versionElements = dependency.getElementsByTagName("version");
	        if (versionElements.getLength() > 0) {
	        	this.versionId = dependency.getElementsByTagName("version").item(0).getTextContent();
	        }
	        else {
	        	this.versionId = "";
	        }
	        
	        //parsing for exclusions
	        List<Element> exclusionList = XmlUtils.findElements("exclusions/exclusion", dependency);
	        
	        if(exclusionList.size() > 0) {
	        	for (Element exclusion : exclusionList) {
	        		Element exclusionE= XmlUtils.findFirstElement("groupId", exclusion);
	        		String exclusionId = "";
	        		if (exclusionE !=null) {
	        			exclusionId = exclusionE.getTextContent();
	        		}
	        		Element exclusionArtifactE = XmlUtils.findFirstElement("artifactId", exclusion);
	        		String exclusionArtifactId = "";
	        		if (exclusionArtifactE !=null) {
	        			exclusionArtifactId = exclusionArtifactE.getTextContent();
	        		}
	        		if(!(exclusionArtifactId.length() < 1) && !(exclusionId.length() < 1)) {
		        		this.exclusions.add(new Dependency(exclusionId, exclusionArtifactId, "ignored"));
	        		}
	        	}
	        }
	    } 
        //otherwise test for Ivy format
        else if (dependency.hasAttribute("org") && dependency.hasAttribute("name") && dependency.hasAttribute("rev")){
        	this.groupId = new JavaPackage(dependency.getAttribute("org"));
        	this.artifactId = new JavaSymbolName(dependency.getAttribute("name"));
        	this.versionId = dependency.getAttribute("rev");
        	//TODO: implement exclusions parser for IVY format
        } else {
        	throw new IllegalStateException("Dependency XML format not supported or is missing a mandatory node ('" + dependency + "')");
        }
	}
	
	/**
	 * Creates an immutable {@link Dependency}.
	 * 
	 * @param groupId the group ID (required)
	 * @param artifactId the artifact ID (required)
	 * @param versionId the version ID (required)
	 * @param type the dependency type (required)
	 */
	public Dependency(JavaPackage groupId, JavaSymbolName artifactId, String versionId, DependencyType type) {
		Assert.notNull(groupId, "Group ID required");
		Assert.notNull(artifactId, "Artifact ID required");
		Assert.notNull(versionId, "Version ID required");
		Assert.notNull(type, "Dependency type required");
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.versionId = versionId;
		this.type = type;
	}

	public JavaPackage getGroupId() {
		return groupId;
	}

	public JavaSymbolName getArtifactId() {
		return artifactId;
	}

	public String getVersionId() {
		return versionId;
	}

	public DependencyType getType() {
		return type;
	}
	
	/**
	 * @return list of exclusions (never null)
	 */
	public List<Dependency> getExclusions() {
		return exclusions;
	}
	
	public int hashCode() {
		return 11 * this.groupId.hashCode() * this.artifactId.hashCode() * this.versionId.hashCode() * this.type.hashCode();
	}

	public boolean equals(Object obj) {
		return obj != null && obj instanceof Dependency && this.compareTo((Dependency)obj) == 0;
	}

	public int compareTo(Dependency o) {
		if (o == null) {
			throw new NullPointerException();
		}
		int result = this.groupId.compareTo(o.groupId);
		if (result == 0) {
			result = this.artifactId.compareTo(o.artifactId);
		}
		if (result == 0) {
			result = this.versionId.compareTo(o.versionId);
		}
		if (result == 0) {
			result = this.type.compareTo(o.type);
		}
		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("groupId", groupId);
		tsc.append("artifactId", artifactId);
		tsc.append("versionId", versionId);
		tsc.append("type", type);
		return tsc.toString();
	}

}
