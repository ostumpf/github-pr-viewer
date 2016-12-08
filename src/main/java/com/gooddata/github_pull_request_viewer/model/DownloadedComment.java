package com.gooddata.github_pull_request_viewer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DownloadedComment extends Comment {
    private final int lineNumber;
    private final User user;

    @JsonCreator
    public DownloadedComment(@JsonProperty("body") final String body,
                             @JsonProperty("commit_id") final String commit,
                             @JsonProperty("path") final String path, @JsonProperty("position") final int position,
                             @JsonProperty("user") final User user,
                             @JsonProperty("original_position") final int lineNumber) {
        super(body, commit, path, position);
        this.lineNumber = lineNumber;
        this.user = user;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public User getUser() {
        return user;
    }
}
