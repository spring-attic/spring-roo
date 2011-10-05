package org.springframework.roo.addon.roobot.client.model;

import java.util.Date;

public class Comment {
	private final Rating rating;
	private final String comment;
	private final Date date;

	public Comment(final Rating rating, final String comment, final Date date) {
		super();
		this.rating = rating;
		this.comment = comment;
		this.date = date;
	}

	public Rating getRating() {
		return rating;
	}

	public String getComment() {
		return comment;
	}

	public Date getDate() {
		return date;
	}
}
