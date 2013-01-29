package org.springframework.roo.classpath.details.comments;

import java.util.List;

/**
 * @author Mike De Haan
 */
public class CommentStructure {

    private List<Comment> beginComments;
    private List<Comment> internalComments;
    private List<Comment> endComments;

    public List<Comment> getBeginComments() {
        return beginComments;
    }

    public void setBeginComments(List<Comment> beginComments) {
        this.beginComments = beginComments;
    }

    public List<Comment> getInternalComments() {
        return internalComments;
    }

    public void setInternalComments(List<Comment> internalComments) {
        this.internalComments = internalComments;
    }

    public List<Comment> getEndComments() {
        return endComments;
    }

    public void setEndComments(List<Comment> endComments) {
        this.endComments = endComments;
    }
}
