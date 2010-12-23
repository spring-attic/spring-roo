package org.springframework.roo.addon.roobot.client.model;

import java.util.Date;

public class Comment {
	
	private Rating rating;
	private String comment;
	private Date date;

	public Comment(Rating rating, String comment, Date date) {
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
