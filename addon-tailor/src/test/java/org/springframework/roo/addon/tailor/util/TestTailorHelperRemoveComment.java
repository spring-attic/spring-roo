package org.springframework.roo.addon.tailor.util;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Tests for {@link TailorHelper#removeComment(String, boolean)}
 * 
 * @author Vladimir Tihomirov
 */
public class TestTailorHelperRemoveComment {

    /**
     * Tests a block comment in the line
     **/
    @Test
    public void testBlockLine() {
        final CommentedLine comment = new CommentedLine("test/*comment*/test",
                false);
        TailorHelper.removeComment(comment);
        Assert.assertEquals("Unexpected result: " + comment.getLine(),
                "testtest", comment.getLine());
        Assert.assertFalse(comment.getInBlockComment());
    }

    /**
     * Tests a block comment in the script
     **/
    @Test
    public void testBlockScript() {
        CommentedLine comment = new CommentedLine("start/*script comment",
                false);
        TailorHelper.removeComment(comment);
        Assert.assertEquals("Unexpected result: " + comment.getLine(), "start",
                comment.getLine());
        Assert.assertTrue(comment.getInBlockComment());
        comment = new CommentedLine("inblock comment", true);
        TailorHelper.removeComment(comment);
        Assert.assertEquals("Unexpected result: " + comment.getLine(), "",
                comment.getLine());
        Assert.assertTrue(comment.getInBlockComment());
        comment = new CommentedLine("close comment*/stop", true);
        TailorHelper.removeComment(comment);
        Assert.assertEquals("Unexpected result: " + comment.getLine(), "stop",
                comment.getLine());
        Assert.assertFalse(comment.getInBlockComment());
    }

    /**
     * Tests a inline comment
     **/
    @Test
    public void testInLineHash() {
        final CommentedLine comment = new CommentedLine("#comment", false);
        TailorHelper.removeComment(comment);
        Assert.assertEquals("Unexpected result: " + comment.getLine(), "",
                comment.getLine());
        Assert.assertFalse(comment.getInBlockComment());
    }

    /**
     * Tests a inline comment
     **/
    @Test
    public void testInLineSlash() {
        final CommentedLine comment = new CommentedLine("//comment", false);
        TailorHelper.removeComment(comment);
        Assert.assertEquals("Unexpected result: " + comment.getLine(), "",
                comment.getLine());
        Assert.assertFalse(comment.getInBlockComment());
    }
}
