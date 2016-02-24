package org.springframework.roo.metadata;

/**
 * Utility class to model a dependency between different metadata identification
 * strings.
 * <p/>
 * This class contains the information about a metadata dependency only, it
 * doesn't neither register nor deregister the dependency automatically.
 * <p/>
 * To register a metadata dependency you must call
 * {@link MetadataDependencyRegistry}.
 * <p/>
 * About the terms "upstream" and "downstream", we use them to refer to these
 * dependencies. For example, an item of metadata representing the members
 * appearing within a Java source file would "depend on" the metadata that
 * represents the physical source file on disk. An item of metadata can also be
 * "depended on" by other pieces of metadata. In this example we would say that
 * Java member metadata is downstream of the physical source file on disk
 * metadata. We would also say that the physical source file on disk is upstream
 * of the Java member metadata.
 * 
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
public class MetadataDependency {

  protected String upstreamDependency;
  protected String downstreamDependency;

  /**
   * @param upstream the upstream dependency (required)
   * @param downstream the downstream dependency (required)
   */
  public MetadataDependency(String upstream, String downstream) {
    this.upstreamDependency = upstream;
    this.downstreamDependency = downstream;
  }

  /**
   * @return the downstreamDependency
   */
  public String getDownstreamDependency() {
    return downstreamDependency;
  }

  /**
   * @param downstreamDependency the downstreamDependency to set
   */
  public void setDownstreamDependency(String downstreamDependency) {
    this.downstreamDependency = downstreamDependency;
  }

  /**
   * @return the upstreamDependency
   */
  public String getUpstreamDependency() {
    return upstreamDependency;
  }

  /**
   * @param upstreamDependency the upstreamDependency to set
   */
  public void setUpstreamDependency(String upstreamDependency) {
    this.upstreamDependency = upstreamDependency;
  }

}
