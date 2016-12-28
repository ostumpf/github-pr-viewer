package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.diff_parser.Diff;
import com.gooddata.github_pull_request_viewer.model.DownloadedComment;
import com.gooddata.github_pull_request_viewer.model.HighlightedRow;
import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.branch.GitBranchUtil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CodeReviewService {

    private String githubAuthorization;
    private PullRequest pullRequest;
    private List<Diff> diffs;
    private List<DownloadedComment> comments;
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

    public List<DownloadedComment> getComments() {
        return comments;
    }

    public void setComments(List<DownloadedComment> comments) {
        this.comments = comments;
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

    public Optional<Diff> getDiff(final Project project, final VirtualFile virtualFile) {
        final String relativeFilePath = getRelativePath(project, virtualFile);

        return getDiff(relativeFilePath);
    }

    public Optional<Diff> getDiff(final String relativeFilePath) {
        return diffs.stream().filter(diff -> diff.getRelativePath().equals(relativeFilePath)).findFirst();
    }

    public List<DownloadedComment> getCommentsForFile(final Project project, final VirtualFile virtualFile) {
        final String relativeFilePath = getRelativePath(project, virtualFile);

        return comments.stream().filter(c -> c.getPath().equals(relativeFilePath)).collect(Collectors.toList());
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

    private String getRelativePath(final Project project, final VirtualFile virtualFile) {
        final VirtualFile defaultRoot = GitBranchUtil.getCurrentRepository(project).getRoot();
        return new File(defaultRoot.getPath())
                .toURI()
                .relativize(new File(virtualFile.getPath()).toURI())
                .toString();
    }
}
