package org.springframework.roo.addon.tailor.util;

public class CommentedLine {
    private String line;
    private Boolean inBlockComment;

    public CommentedLine(final String line, final Boolean inBlockComment) {
        this.line = line;
        this.inBlockComment = inBlockComment;
    }

    public Boolean getInBlockComment() {
        return inBlockComment;
    }

    public String getLine() {
        return line;
    }

    public void setInBlockComment(final Boolean inBlockComment) {
        this.inBlockComment = inBlockComment;
    }

    public void setLine(final String line) {
        this.line = line;
    }
}
