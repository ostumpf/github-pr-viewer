package com.gooddata.github_pull_request_viewer.model;

public class PullRequest {

    private final String repoOwner;
    private final String repoName;
    private final String pullRequestId;

    public PullRequest(final String repoOwner, final String repoName, final String pullRequestId) {
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.pullRequestId = pullRequestId;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getPullRequestId() {
        return pullRequestId;
    }
}
