package org.springframework.roo.classpath.details.comments;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.support.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Mike De Haan
 */
public class CommentStructure {

  public enum CommentLocation {
    BEGINNING, INTERNAL, END
  }

  private List<AbstractComment> beginComments;
  private List<AbstractComment> endComments;
  private List<AbstractComment> internalComments;

  public CommentStructure() {
    super();
  }

  public CommentStructure(AbstractComment comment) {
    this();
    addComment(comment, CommentLocation.BEGINNING);
  }

  /**
   * Helper method to assist in adding comments to structures.
   *
   * @param comment The comment to add (LineComment, BlockComment,
   *            JavadocComment)
   * @param commentLocation Where the comment should be added.
   */
  public void addComment(AbstractComment comment, CommentLocation commentLocation) {

    Validate.notNull(comment, "Comment must not be null");
    Validate.notNull(comment, "Comment location must be specified");

    if (commentLocation.equals(CommentLocation.BEGINNING)) {
      if (beginComments == null) {
        beginComments = new LinkedList<AbstractComment>();
      }

      beginComments.add(comment);
    } else if (commentLocation.equals(CommentLocation.INTERNAL)) {
      if (internalComments == null) {
        internalComments = new LinkedList<AbstractComment>();
      }

      internalComments.add(comment);
    } else {
      if (endComments == null) {
        endComments = new LinkedList<AbstractComment>();
      }

      endComments.add(comment);
    }
  }

  public List<AbstractComment> getBeginComments() {
    return beginComments;
  }

  public List<AbstractComment> getEndComments() {
    return endComments;
  }

  public List<AbstractComment> getInternalComments() {
    return internalComments;
  }

  public void setBeginComments(final List<AbstractComment> beginComments) {
    this.beginComments = beginComments;
  }

  public void setEndComments(final List<AbstractComment> endComments) {
    this.endComments = endComments;
  }

  public void setInternalComments(final List<AbstractComment> internalComments) {
    this.internalComments = internalComments;
  }

  public String asPlainText() {
    StringBuilder sb = new StringBuilder();
    if (!CollectionUtils.isEmpty(beginComments)) {
      sb.append(StringUtils.join(beginComments, "\n"));
    }
    if (!CollectionUtils.isEmpty(internalComments)) {
      if (sb.length() > 0) {
        sb.append("/n");
      }
      sb.append(StringUtils.join(internalComments, "\n"));
    }
    if (!CollectionUtils.isEmpty(endComments)) {
      if (sb.length() > 0) {
        sb.append("/n");
      }
      sb.append(StringUtils.join(endComments, "\n"));
    }
    return sb.toString();
  }

  public List<AbstractComment> asList() {
    List<AbstractComment> comments = new ArrayList<AbstractComment>();
    if (!isEmpty(beginComments)) {
      comments.addAll(beginComments);
    }
    if (!isEmpty(internalComments)) {
      comments.addAll(internalComments);
    }
    if (!isEmpty(endComments)) {
      comments.addAll(endComments);
    }
    return comments;
  }

  public boolean isEmpty() {
    return isEmpty(beginComments) && isEmpty(internalComments) && isEmpty(endComments);
  }

  private boolean isEmpty(List<AbstractComment> comments) {
    if (CollectionUtils.isEmpty(comments)) {
      return true;
    }
    for (AbstractComment comment : comments) {
      if (StringUtils.isNotBlank(comment.getComment())) {
        return false;
      }
    }
    return true;
  }
}
