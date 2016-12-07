package com.gooddata.github_pull_request_viewer.model;

public class PullRequestSource {

    private final String remoteUserName;
    private final String remoteBranch;

    public PullRequestSource(String remoteUserName, String remoteBranch) {
        this.remoteUserName = remoteUserName;
        this.remoteBranch = remoteBranch;
    }

    public String getRemoteUserName() {
        return remoteUserName;
    }

    public String getRemoteUrl() {
        return String.format("git@github.com:%s/a-team-weaponry.git", remoteUserName);
    }

    public String getRemoteBranch() {
        return remoteBranch;
    }
}
