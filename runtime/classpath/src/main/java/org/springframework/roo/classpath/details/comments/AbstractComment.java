package org.springframework.roo.classpath.details.comments;

/**
 * @author Mike De Haan
 */
public abstract class AbstractComment {

    private String comment;

    protected AbstractComment() {
    }

    protected AbstractComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
