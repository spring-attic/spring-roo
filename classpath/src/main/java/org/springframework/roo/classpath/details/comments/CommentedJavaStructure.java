package org.springframework.roo.classpath.details.comments;

/**
 * Metadata concerning comments
 *
 * @author Mike De Haan
 * @since 1.3
 */
public interface CommentedJavaStructure {

    CommentStructure getCommentStructure();

    void setCommentStructure(CommentStructure commentStructure);

}
