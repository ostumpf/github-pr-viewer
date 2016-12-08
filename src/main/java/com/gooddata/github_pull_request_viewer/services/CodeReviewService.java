package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.Comment;
import com.gooddata.github_pull_request_viewer.model.DownloadedComment;
import com.gooddata.github_pull_request_viewer.model.PullRequest;
import org.wickedsource.diffparser.api.model.Diff;

import java.util.List;

public class CodeReviewService {

    private String githubToken;
    private PullRequest pullRequest;
    private List<Diff> diffs;
    private List<DownloadedComment> comments;

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public List<Diff> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<Diff> diffs) {
        this.diffs = diffs;
    }

    public List<DownloadedComment> getComments() {
        return comments;
    }

    public void setComments(List<DownloadedComment> comments) {
        this.comments = comments;
    }

    public boolean inProgress() {
        return diffs != null;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(final String githubToken) {
        this.githubToken = githubToken;
    }
}
