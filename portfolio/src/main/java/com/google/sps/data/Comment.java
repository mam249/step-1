package com.google.sps.data;

public final class Comment {

  private final long id;
  private final String name;
  private final String comment;
  private final long timestamp;

  public Comment(long id, String name, String comment, long timestamp) {
    this.id = id;
    this.name = name;
    this.comment = comment;
    this.timestamp = timestamp;
  }
}
