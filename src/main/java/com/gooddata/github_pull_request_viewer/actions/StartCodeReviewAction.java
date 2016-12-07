package com.gooddata.github_pull_request_viewer.actions;

import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.gooddata.github_pull_request_viewer.services.FileHighlightService;
import com.gooddata.github_pull_request_viewer.services.GitHubRestService;
import com.gooddata.github_pull_request_viewer.utils.Gui;
import com.gooddata.github_pull_request_viewer.utils.RegexUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.branch.GitBranchUtil;
import git4idea.branch.GitBrancher;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.update.GitFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.util.AuthLevel;
import org.jetbrains.plugins.github.util.GithubAuthDataHolder;
import org.jetbrains.plugins.github.util.GithubNotifications;
import org.jetbrains.plugins.github.util.GithubUtil;
import org.wickedsource.diffparser.api.UnifiedDiffParser;
import org.wickedsource.diffparser.api.model.Diff;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static git4idea.actions.GitRepositoryAction.getGitRoots;

public class StartCodeReviewAction extends AnAction {

    private static final Logger logger = Logger.getInstance(StartCodeReviewAction.class);

    /*public static void main(String[] args) {
        try {
            final List<Diff> diffs = new StartCodeReviewAction().getPullRequestDiffs(new PullRequest("gooddata", "a-team-weaponry", "106"), "");
            final Diff diff = diffs.get(0);
            diff.getHeaderLines().forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    @Override
    public void update(AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final FileHighlightService fileHighlightService =
                ServiceManager.getService(e.getProject(), FileHighlightService.class);

        e.getPresentation().setEnabled(fileHighlightService.getDiffs() == null);
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            logger.warn("action=start_code_review the project is null, not doing anything");
            return;
        }

        final String pullRequestUrl = Gui.getGitHubPullRequestUrl(e.getProject());
        final PullRequest pullRequest = RegexUtils.getPullRequest(pullRequestUrl);

        logger.info("action=start_code_review status=start");

        final FileHighlightService fileHighlightService =
                ServiceManager.getService(project,
                        FileHighlightService.class);

        try {
            GithubUtil.computeValueInModal(project, "Access to GitHub", indicator -> {
                final GitRepository repository = GithubUtil.getGitRepository(project,
                        e.getData(CommonDataKeys.VIRTUAL_FILE));
                indicator.setFraction(0);
                indicator.setText("checking github token");

                final String githubToken;
                try {
                    final GithubAuthDataHolder authDataHolder = GithubUtil.getValidAuthDataHolderFromConfig(project,
                            AuthLevel.TOKEN, indicator);
                    githubToken = authDataHolder.getAuthData().getTokenAuth().getToken();
                } catch (final Exception ex1) {
                    throw new IllegalStateException("failed to retrieve token");
                }

                indicator.setFraction(0.40);
                indicator.setText("loading pull request diffs");

                try {
                    final List<Diff> diffs = getPullRequestDiffs(project, pullRequest, githubToken);
                    fileHighlightService.setDiffs(diffs);
                } catch (final Exception ex) {
                    logger.warn(ex);
                    fileHighlightService.setDiffs(null);

                    Messages.showErrorDialog(e.getProject(), ex.getMessage(), "Error");
                    throw new IllegalStateException("failed to load diffs from pull request");
                }

                indicator.setText("highlighting the changes");
                indicator.setFraction(0.90);

                final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                fileHighlightService.highlightFile(fileEditorManager);

                indicator.setText("checking out the branch");
                indicator.setFraction(0.95);

                // remotes
                final Git git = ServiceManager.getService(project, Git.class);
                final String remoteName = "newremote";
                final String remoteUrl = "git@github.com:gooddata/a-team-weaponry.git";
                git.addRemote(repository, remoteName, remoteUrl);

                // fetch
                FileDocumentManager.getInstance().saveAllDocuments();
                final GitVcs vcs = GitVcs.getInstance(project);
                final List<VirtualFile> gitRoots = getGitRoots(project, vcs);
                if (gitRoots == null) throw new IllegalStateException("cannot determine git root folder");

                final VirtualFile defaultRoot = GitBranchUtil.getCurrentRepository(project).getRoot();
                GitVcs.runInBackground(new Task.Backgroundable(project, "Fetching...", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
                        new GitFetcher(project, indicator, true).fetchRootsAndNotify(GitUtil.getRepositoriesFromRoots(repositoryManager, gitRoots),
                                null, true);
                    }
                });

                // checkout
                final String branchName = "develop";
                final GitBrancher brancher = ServiceManager.getService(project, GitBrancher.class);
                brancher.checkout(branchName, true, Collections.singletonList(repository), null);

                indicator.setText("done");
                indicator.setFraction(1.00);
                logger.info("action=start_code_review status=finished");
                return null;
            });
        } catch(IllegalStateException ex) {
            GithubNotifications.showError(project, "error", ex.getMessage());
        }
    }

    private List<Diff> getPullRequestDiffs(final Project project, final PullRequest pullRequest, final String githubToken) throws IOException {
        final GitHubRestService gitHubRestService = ServiceManager.getService(project, GitHubRestService.class);

        return new UnifiedDiffParser().parse(gitHubRestService.getPullRequestDiff(pullRequest, githubToken));
    }
}
