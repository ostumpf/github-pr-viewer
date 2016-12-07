package com.gooddata.github_pull_request_viewer.model;

public class PullRequestSource {

    private final String remoteUserName;
    private final String remoteBranch;
    private final String repoName;

    public PullRequestSource(final String repoName, final String remoteUserName, final String remoteBranch) {
        this.repoName = repoName;
        this.remoteUserName = remoteUserName;
        this.remoteBranch = remoteBranch;
    }

    public String getRemoteUserName() {
        return remoteUserName;
    }

    public String getRemoteUrl() {
        return String.format("git@github.com:%s/%s.git", remoteUserName, repoName);
    }

    public String getRemoteBranch() {
        return remoteBranch;
    }
}
