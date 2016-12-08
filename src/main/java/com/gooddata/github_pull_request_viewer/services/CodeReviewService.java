package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.HighlightedRow;
import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.gooddata.github_pull_request_viewer.utils.RegexUtils;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.wickedsource.diffparser.api.model.Diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeReviewService {

    private static final Logger logger = Logger.getInstance(CodeReviewService.class);

    private String githubAuthorization;
    private PullRequest pullRequest;
    private List<Diff> diffs;
    private final Map<VirtualFile, Map<Integer, HighlightedRow>> highlightedRowsMap = new HashMap<>();

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

    public Map<VirtualFile, Map<Integer, HighlightedRow>> getHighlightedRowsMap() {
        return highlightedRowsMap;
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

    public String getRelativePath(final Diff diff) {
        final List<String> headers = diff.getHeaderLines();
        if (headers.size() < 1) {
            logger.warn("action=get_target_commit cannot determine target commit for diff");
            return null;
        }

        return RegexUtils.getRelativeFilePath(headers.get(0));
    }
}
