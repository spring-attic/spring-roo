package org.springframework.roo.addon.roobot.client.model;

import java.util.Date;

public class Comment {
    private final String comment;
    private final Date date;
    private final Rating rating;

    public Comment(final Rating rating, final String comment, final Date date) {
        super();
        this.rating = rating;
        this.comment = comment;
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public Date getDate() {
        return date;
    }

    public Rating getRating() {
        return rating;
    }
}
