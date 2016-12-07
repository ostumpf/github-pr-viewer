package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.PullRequest;
import org.wickedsource.diffparser.api.model.Diff;

import java.util.List;

public class CodeReviewService {

    private PullRequest pullRequest;
    private List<Diff> diffs;

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

    public boolean inProgress() {
        return diffs != null;
    }
}
