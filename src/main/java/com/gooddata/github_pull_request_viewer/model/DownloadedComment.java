package com.gooddata.github_pull_request_viewer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DownloadedComment extends Comment {
    private final int lineNumber;

    @JsonCreator
    public DownloadedComment(@JsonProperty("body") String body,
                             @JsonProperty("commit_id") String commit,
                             @JsonProperty("path") String path, @JsonProperty("position") int position,
                             @JsonProperty("original_position") int lineNumber) {
        super(body, commit, path, position);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
