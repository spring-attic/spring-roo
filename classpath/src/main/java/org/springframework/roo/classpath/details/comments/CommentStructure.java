package org.springframework.roo.classpath.details.comments;

import org.apache.commons.lang3.Validate;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Mike De Haan
 */
public class CommentStructure {

    public static enum CommentLocation {
        BEGINNING, INTERNAL, END
    }

    private List<AbstractComment> beginComments;
    private List<AbstractComment> endComments;
    private List<AbstractComment> internalComments;

    /**
     * Helper method to assist in adding comments to structures.
     * 
     * @param comment The comment to add (LineComment, BlockComment,
     *            JavadocComment)
     * @param commentLocation Where the comment should be added.
     */
    public void addComment(AbstractComment comment,
            CommentLocation commentLocation) {

        Validate.notNull(comment, "Comment must not be null");
        Validate.notNull(comment, "Comment location must be specified");

        if (commentLocation.equals(CommentLocation.BEGINNING)) {
            if (beginComments == null) {
                beginComments = new LinkedList<AbstractComment>();
            }

            beginComments.add(comment);
        }
        else if (commentLocation.equals(CommentLocation.INTERNAL)) {
            if (internalComments == null) {
                internalComments = new LinkedList<AbstractComment>();
            }

            internalComments.add(comment);
        }
        else {
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
}
