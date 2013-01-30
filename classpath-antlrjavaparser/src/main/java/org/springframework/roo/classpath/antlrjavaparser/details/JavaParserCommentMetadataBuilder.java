package org.springframework.roo.classpath.antlrjavaparser.details;

import java.util.LinkedList;
import java.util.List;

import org.springframework.roo.classpath.details.comments.CommentStructure;

import com.github.antlrjavaparser.api.BlockComment;
import com.github.antlrjavaparser.api.Comment;
import com.github.antlrjavaparser.api.LineComment;
import com.github.antlrjavaparser.api.Node;
import com.github.antlrjavaparser.api.body.JavadocComment;

/**
 * @author Mike De Haan
 */
public class JavaParserCommentMetadataBuilder {

    /**
     * Adapt the any comments to the roo interface
     * 
     * @param parserNode The antlr-java-parser node from which the comments will
     *            be read
     * @param commentStructure The roo structure from which to retrieve comments
     * @return List of comments from the antlr-java-parser package
     */
    public static void updateCommentsToRoo(
            final CommentStructure commentStructure, final Node parserNode) {

        // Nothing to do here
        if (parserNode == null || commentStructure == null) {
            return;
        }

        commentStructure.setBeginComments(adaptToRooComments(parserNode
                .getBeginComments()));
        commentStructure.setInternalComments(adaptToRooComments(parserNode
                .getInternalComments()));
        commentStructure.setEndComments(adaptToRooComments(parserNode
                .getEndComments()));
    }

    /**
     * Adapt the any comments to the antlr-java-parser interface
     * 
     * @param parserNode The antlr-java-parser node to where the comments will
     *            be set
     * @param commentStructure The roo structure from which to retrieve comments
     * @return List of comments from the antlr-java-parser package
     */
    public static void updateCommentsToJavaParser(final Node parserNode,
            final CommentStructure commentStructure) {

        // Nothing to do here
        if (parserNode == null || commentStructure == null) {
            return;
        }

        parserNode.setBeginComments(adaptComments(commentStructure
                .getBeginComments()));
        parserNode.setInternalComments(adaptComments(commentStructure
                .getInternalComments()));
        parserNode.setEndComments(adaptComments(commentStructure
                .getEndComments()));
    }

    /**
     * Adapt a roo comment to antlr-java-parser comment
     * 
     * @param antlrComments List of comments from the antlr-java-parser package
     * @return List of comments from the roo package
     */
    private static List<org.springframework.roo.classpath.details.comments.AbstractComment> adaptToRooComments(
            final List<Comment> antlrComments) {

        // Nothing to do here
        if (antlrComments == null || antlrComments.size() == 0) {
            return null;
        }

        final List<org.springframework.roo.classpath.details.comments.AbstractComment> comments = new LinkedList<org.springframework.roo.classpath.details.comments.AbstractComment>();
        for (final Comment antlrComment : antlrComments) {
            comments.add(adaptToRooComment(antlrComment));
        }

        return comments;
    }

    /**
     * Adapt a roo comment to antlr-java-parser comment
     * 
     * @param antlrComment
     * @return
     */
    private static org.springframework.roo.classpath.details.comments.AbstractComment adaptToRooComment(
            final Comment antlrComment) {
        org.springframework.roo.classpath.details.comments.AbstractComment comment;

        if (antlrComment instanceof LineComment) {
            comment = new org.springframework.roo.classpath.details.comments.LineComment();
        }
        else if (antlrComment instanceof JavadocComment) {
            comment = new org.springframework.roo.classpath.details.comments.JavadocComment();
        }
        else {
            comment = new org.springframework.roo.classpath.details.comments.BlockComment();
        }

        comment.setComment(antlrComment.getContent());

        return comment;
    }

    /**
     * Adapt the roo interface to the antlr-java-parser interface
     * 
     * @param rooComments List of comments from the roo package
     * @return List of comments from the antlr-java-parser package
     */
    private static List<Comment> adaptComments(
            final List<org.springframework.roo.classpath.details.comments.AbstractComment> rooComments) {

        // Nothing to do here
        if (rooComments == null || rooComments.size() == 0) {
            return null;
        }

        final List<Comment> comments = new LinkedList<Comment>();
        for (final org.springframework.roo.classpath.details.comments.AbstractComment rooComment : rooComments) {
            comments.add(adaptComment(rooComment));
        }

        return comments;
    }

    /**
     * Adapt the roo interface to the antlr-java-parser interface
     * 
     * @param rooComment
     * @return
     */
    private static Comment adaptComment(
            final org.springframework.roo.classpath.details.comments.AbstractComment rooComment) {
        Comment comment;

        if (rooComment instanceof org.springframework.roo.classpath.details.comments.LineComment) {
            comment = new LineComment();
        }
        else if (rooComment instanceof org.springframework.roo.classpath.details.comments.JavadocComment) {
            comment = new JavadocComment();
        }
        else {
            comment = new BlockComment();
        }

        comment.setContent(rooComment.getComment());

        return comment;
    }
}
