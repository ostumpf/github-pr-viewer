package com.gooddata.github_pull_request_viewer.actions;

import com.gooddata.github_pull_request_viewer.model.Comment;
import com.gooddata.github_pull_request_viewer.model.DownloadedComment;
import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.gooddata.github_pull_request_viewer.model.PullRequestSource;
import com.gooddata.github_pull_request_viewer.services.CodeReviewService;
import com.gooddata.github_pull_request_viewer.services.FileHighlightService;
import com.gooddata.github_pull_request_viewer.services.GitHubRestService;
import com.gooddata.github_pull_request_viewer.utils.Gui;
import com.gooddata.github_pull_request_viewer.utils.RegexUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitVcs;
import git4idea.actions.BasicAction;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBrancher;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import git4idea.update.GitFetcher;
import org.apache.commons.net.util.Base64;
import org.jetbrains.plugins.github.util.AuthLevel;
import org.jetbrains.plugins.github.util.GithubAuthData;
import org.jetbrains.plugins.github.util.GithubAuthDataHolder;
import org.jetbrains.plugins.github.util.GithubNotifications;
import org.jetbrains.plugins.github.util.GithubUtil;
import org.wickedsource.diffparser.api.UnifiedDiffParser;
import org.wickedsource.diffparser.api.model.Diff;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static git4idea.actions.GitRepositoryAction.getGitRoots;

public class StartCodeReviewAction extends AnAction {

    private static final Logger logger = Logger.getInstance(StartCodeReviewAction.class);

    @Override
    public void update(AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final CodeReviewService codeReviewService =
                ServiceManager.getService(e.getProject(), CodeReviewService.class);

        e.getPresentation().setEnabled(!codeReviewService.inProgress());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            logger.warn("action=start_code_review the project is null, not doing anything");
            return;
        }

        final String pullRequestUrl = Gui.getGitHubPullRequestUrl(e.getProject());
        if (pullRequestUrl == null) {
            logger.warn("action=start_code_review User cancelled");
            return;
        }

        final PullRequest pullRequest = RegexUtils.getPullRequest(pullRequestUrl);

        logger.info("action=start_code_review status=start");

        final FileHighlightService fileHighlightService =
                ServiceManager.getService(project, FileHighlightService.class);
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);
        final GitHubRestService gitHubRestService =
                ServiceManager.getService(project, GitHubRestService.class);

        codeReviewService.setPullRequest(pullRequest);

        final GitRepository repository = GithubUtil.getGitRepository(project, null);
        final VirtualFile defaultRoot = GitBranchUtil.getCurrentRepository(project).getRoot();


        GithubUtil.computeValueInModal(project, "Access to GitHub", indicator -> {
            try {
                indicator.setFraction(0);
                indicator.setText("checking github authorization");
                final String githubAuthorization = requestGithubAuthorization(project, indicator);
                codeReviewService.setGithubAuthorization(githubAuthorization);

                final Git git = ServiceManager.getService(project, Git.class);
                final PullRequestSource pullRequestSource = getPullRequestSource(project);

                indicator.setText("creating a remote");
                indicator.setFraction(0.01);
                git.addRemote(repository, pullRequestSource.getRemoteUserName(), pullRequestSource.getRemoteUrl());
                sleep();

                indicator.setText("fetching the remote");
                indicator.setFraction(0.02);
                fetchRemote(project, pullRequestSource.getRemoteUserName(), pullRequestSource.getRemoteBranch(), defaultRoot);

                indicator.setText("checking out the branch");
                indicator.setFraction(0.3);
                checkoutBranch(project, repository, pullRequestSource.getRemoteBranch());

                indicator.setFraction(0.60);
                indicator.setText("loading pull request diffs");
                loadPullRequestDiffs(project, codeReviewService);

                indicator.setFraction(0.75);
                indicator.setText("loading pull request comments");
                loadPullRequestComments(project, codeReviewService, gitHubRestService);

                indicator.setText("highlighting the changes");
                indicator.setFraction(0.90);
                highlightChanges(project, fileHighlightService);

                indicator.setText("done");
                indicator.setFraction(1.00);

            } catch (final Exception ex) {
                GithubNotifications.showError(project, "error", ex.getMessage());
                codeReviewService.stopCodeReview(project);
            }

            return null;
        });
    }

    private void sleep() {
        try {
            Thread.sleep(500l);
        } catch (final InterruptedException ignored) {}
    }

    private void checkoutBranch(final Project project,
                                final GitRepository repository,
                                final String branchName) {
        ServiceManager.getService(project, GitBrancher.class)
                .checkout(branchName,
                true, Collections.singletonList(repository), null);
    }

    private void fetchRemote(final Project project, final String remoteName, final String branchName, final VirtualFile defaultRoot) {
        ApplicationManager.getApplication().invokeLater(BasicAction::saveAll);

        final GitVcs vcs = GitVcs.getInstance(project);
        final List<VirtualFile> gitRoots = getGitRoots(project, vcs);
        if (gitRoots == null) throw new IllegalStateException("cannot determine git root folder");

        new GitFetcher(project, new EmptyProgressIndicator(), true)
                .fetch(defaultRoot, remoteName, branchName);
    }

    private PullRequestSource getPullRequestSource(final Project project) throws IOException {
        final GitHubRestService gitHubRestService = ServiceManager.getService(project, GitHubRestService.class);

        return gitHubRestService.getPullRequestSource(project);
    }

    private void highlightChanges(final Project project, final FileHighlightService fileHighlightService) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        ApplicationManager.getApplication().invokeLater(() ->
                fileHighlightService.highlightFile(fileEditorManager)
        );
    }

    private void loadPullRequestDiffs(final Project project,
                                      final CodeReviewService codeReviewService) throws IOException {
        final List<Diff> diffs = getPullRequestDiffs(project);
        codeReviewService.setDiffs(diffs);
    }

    private String requestGithubAuthorization(final Project project, final ProgressIndicator indicator) throws IOException {
        final GithubAuthDataHolder authDataHolder = GithubUtil.getValidAuthDataHolderFromConfig(project,
                AuthLevel.ANY, indicator);
        if (authDataHolder.getAuthData().getAuthType().equals(GithubAuthData.AuthType.BASIC)) {
            final GithubAuthData.BasicAuth basicAuth = authDataHolder.getAuthData().getBasicAuth();
            return "Basic " + new String(Base64.encodeBase64((basicAuth.getLogin() + ":" + basicAuth.getPassword()).getBytes()));
        } else if (authDataHolder.getAuthData().getAuthType().equals(GithubAuthData.AuthType.TOKEN)) {
            final GithubAuthData.TokenAuth tokenAuth = authDataHolder.getAuthData().getTokenAuth();
            return "token " + tokenAuth.getToken();
        } else {
            throw new IllegalStateException("Failed to retrieve authorization: must be BASIC or TOKEN");
        }
    }

    private void loadPullRequestComments(final Project project, final CodeReviewService codeReviewService, final GitHubRestService gitHubRestService) {
        try {
            final List<DownloadedComment> comments = gitHubRestService.getComments(project);
            codeReviewService.setComments(comments);
        } catch (final IOException e) {
            throw new IllegalStateException("failed to load comments for pull request");
        }
    }

    private List<Diff> getPullRequestDiffs(final Project project) throws IOException {
        final GitHubRestService gitHubRestService = ServiceManager.getService(project, GitHubRestService.class);

        return new UnifiedDiffParser().parse(gitHubRestService.getPullRequestDiff(project));
    }
}
