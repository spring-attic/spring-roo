package org.springframework.roo.addon.gwt.scaffold.place;

/**
 * A place in the app focused on the {@link Values} of a particular type of
 * {@link com.google.gwt.valuestore.shared.Record Record}.
 */
public abstract class ScaffoldRecordPlace extends ScaffoldPlace {

  /**
   * The things you do with a record, each of which is a different bookmarkable
   * location in the scaffold app.
   */
  public enum Operation {
    EDIT, DETAILS
  }
  private final String id;

  private final Operation operation;

  /**
   * @param record
   */
  public ScaffoldRecordPlace(String id, Operation operation) {
    assert null != id;
    assert null != operation;

    this.id = id;
    this.operation = operation;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }
    ScaffoldRecordPlace other = (ScaffoldRecordPlace) obj;

    if (!id.equals(other.id)) {
      return false;
    }

    if (!operation.equals(other.operation)) {
      return false;
    }

    return true;
  }

  /**
   * @return the id for this record
   */
  public String getId() {
    return id;
  }

  /**
   * @return what to do with the record here
   */
  public Operation getOperation() {
    return operation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id.hashCode();
    result = prime * result + operation.hashCode();
    return result;
  }
}