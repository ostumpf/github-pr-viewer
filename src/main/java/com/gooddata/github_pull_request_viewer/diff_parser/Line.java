package com.gooddata.github_pull_request_viewer.diff_parser;

public class Line {
    private final String content;
    private final Type type;

    public Line(final String content, final Type type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Line{" +
                "content='" + content + '\'' +
                ", type=" + type +
                '}';
    }

    public  enum Type {
        ADDED, REMOVED, NEUTRAL
    }
}
