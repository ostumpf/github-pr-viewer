package com.gooddata.github_pull_request_viewer.actions;

import com.gooddata.github_pull_request_viewer.services.FileHighlightService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.plugins.github.util.GithubAuthDataHolder;
import org.jetbrains.plugins.github.util.GithubNotifications;
import org.jetbrains.plugins.github.util.GithubUtil;
import org.wickedsource.diffparser.api.UnifiedDiffParser;
import org.wickedsource.diffparser.api.model.Diff;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class StartCodeReviewAction extends AnAction {

    private static final String GITHUB_API_PR_URL_FORMAT = "https://api.github.com/repos/%s/%s/pulls/%s";
    private static final String ACCEPT_V3_DIFF = "application/vnd.github.v3.diff";
    private static final String USER_AGENT = "gooddata";

    private static final Logger logger = Logger.getInstance(StartCodeReviewAction.class);

    private String githubToken;
   /* public static void main(String[] args) {
        try {
            final List<Diff> diffs = new StartCodeReviewAction().getPullRequestDiffs("gooddata", "a-team-weaponry", "106");
            diffs.stream()
                    .filter(d -> d.getFromFileName().endsWith("java/db-users-migration/pom.xml"))
                    .forEach(d ->
                            d.getHunks().forEach(hunk -> {
                                hunk.getLines().forEach(line -> System.out.println(line.getLineType()));
                                System.out.println(hunk.getToFileRange().getLineStart() + ": " + hunk.getToFileRange().getLineCount());
                            }));

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

        final String repoOwner = "gooddata";
        final String repoName = "a-team-weaponry";
        final String pullRequestId = "106";

        logger.info("action=start_code_review status=start");

        final FileHighlightService fileHighlightService =
                ServiceManager.getService(project,
                        FileHighlightService.class);


        try {
            GithubUtil.computeValueInModal(project, "Access to GitHub", indicator -> {
                indicator.setFraction(0);
                indicator.setText("checking github token");

                try {
                    setGithubToken(GithubUtil.getValidAuthDataHolderFromConfig(project, indicator));
                } catch (Exception ex1) {
                    throw new IllegalStateException("failed to retrieve token");
                }

//                GithubNotifications.showInfo(project, "token", "using GitHub token: " + githubToken);
                indicator.setFraction(0.40);
                indicator.setText("loading pull request diffs");

                try {
                    final List<Diff> diffs = getPullRequestDiffs(repoOwner, repoName, pullRequestId);
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

                indicator.setText("done");
                indicator.setFraction(1.00);
                logger.info("action=start_code_review status=finished");
            });
        } catch(IllegalStateException ex) {
            GithubNotifications.showError(project, "error",
                    ex.getMessage());
            return;
        }
    }

    private void setGithubToken(final GithubAuthDataHolder authDataHolder) {
        githubToken = authDataHolder.getAuthData().getTokenAuth().getToken();
    }

    private List<Diff> getPullRequestDiffs(final String repoOwner,
                                           final String repoName,
                                           final String pullRequestId) throws IOException {
        logger.info(format("action=download_diff status=start repo_owner=%s repo_name=%s pull_request_id=%s",
                repoOwner, repoName, pullRequestId));

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_PR_URL_FORMAT, repoOwner, repoName, pullRequestId));
        request.addHeader("Accept", ACCEPT_V3_DIFF);
        request.addHeader("Authorization", "token " + githubToken);
        request.addHeader("User-Agent", USER_AGENT);
        final HttpResponse response =  client.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException("Cannot download the pull request diff from GitHub: " +
                    response.getStatusLine().toString());
        }

        final List<Diff> diffs = new UnifiedDiffParser().parse(response.getEntity().getContent());

        logger.info(format("action=download_diff status=finished repo_owner=%s repo_name=%s pull_request_id=%s",
                repoOwner, repoName, pullRequestId));

        return diffs;
    }
}
