package com.gooddata.github_pull_request_viewer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Comment {

    private final String body;
    private final String commit;
    private final String path;
    private final int position;

    @JsonCreator
    public Comment(@JsonProperty("body") final String body,
                   @JsonProperty("commit_id") final String commit,
                   @JsonProperty("path") final String path,
                   @JsonProperty("position") final int position) {
        this.body = body;
        this.commit = commit;
        this.path = path;
        this.position = position;
    }

    public String getBody() {
        return body;
    }

    @JsonProperty("commit_id")
    public String getCommit() {
        return commit;
    }

    public String getPath() {
        return path;
    }

    public int getPosition() {
        return position;
    }
}
