package org.springframework.roo.classpath.details.comments;

import java.util.List;

/**
 * @author Mike De Haan
 */
public class CommentStructure {

    private List<AbstractComment> beginComments;
    private List<AbstractComment> endComments;
    private List<AbstractComment> internalComments;

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
