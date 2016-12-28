package com.gooddata.github_pull_request_viewer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DownloadedComment extends Comment implements Comparable<DownloadedComment> {

    private static final DateFormat commentDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");

    private final User user;
    private final Date updatedAt;

    @JsonCreator
    public DownloadedComment(@JsonProperty("body") final String body,
                             @JsonProperty("commit_id") final String commit,
                             @JsonProperty("path") final String path, @JsonProperty("position") final int position,
                             @JsonProperty("user") final User user,
                             @JsonProperty("updated_at") final Date updatedAt) {
        super(body, commit, path, position);
        this.user = user;
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getDisplayText() {
        return String.format("[%s %s]: %s", commentDateFormat.format(getUpdatedAt()), getUser().getUsername(), getBody());
    }

    @Override
    public int compareTo(@NotNull DownloadedComment o) {
        return updatedAt.compareTo(o.getUpdatedAt());
    }
}
