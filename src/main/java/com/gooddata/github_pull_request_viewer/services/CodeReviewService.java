package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.wickedsource.diffparser.api.model.Diff;

import java.util.List;

public class CodeReviewService {

    private String githubAuthorization;
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

    public String getGithubAuthorization() {
        return githubAuthorization;
    }

    public void setGithubAuthorization(final String githubAuthorization) {
        this.githubAuthorization = githubAuthorization;
    }

    public void stopCodeReview(final Project project) {
        final FileHighlightService fileHighlightService =
                ServiceManager.getService(project,
                        FileHighlightService.class);

        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        setDiffs(null);
        setPullRequest(null);
        fileHighlightService.highlightFile(fileEditorManager);
    }
}
