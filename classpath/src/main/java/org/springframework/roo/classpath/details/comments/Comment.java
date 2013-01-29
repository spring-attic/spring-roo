package org.springframework.roo.classpath.details.comments;

/**
 * @author Mike De Haan
 */
public abstract class Comment {
    private String comment;

    protected Comment() {

    }

    protected Comment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
