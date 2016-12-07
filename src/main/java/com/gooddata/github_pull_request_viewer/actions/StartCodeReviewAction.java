package com.gooddata.github_pull_request_viewer.actions;

import com.gooddata.github_pull_request_viewer.services.FileHighlightService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.wickedsource.diffparser.api.UnifiedDiffParser;
import org.wickedsource.diffparser.api.model.Diff;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class StartCodeReviewAction extends AnAction {

    private static final String GITHUB_API_PR_URL_FORMAT = "https://api.github.com/repos/%s/%s/pulls/%s";
    private static final String ACCEPT_V3_DIFF = "application/vnd.github.v3.diff";

    private final String githubToken = "TODO";
    private final String githubUsername = "ostumpf";

    /*public static void main(String[] args) {
        new StartCodeReviewAction().actionPerformed(null);
    }*/

    @Override
    public void actionPerformed(final AnActionEvent e) {
        if (e.getProject() == null) {
            System.out.println("action=start_code_review the project is null, not doing anything");
            return;
        }

        final String repoOwner = "gooddata";
        final String repoName = "a-team-weaponry";
        final String pullRequestId = "106";

        System.out.println("action=start_code_review status=start");

        final FileHighlightService fileHighlightService =
                ServiceManager.getService(e.getProject(),
                        FileHighlightService.class);

        try {
            final List<Diff> diffs = getPullRequestDiffs(repoOwner, repoName, pullRequestId);
            fileHighlightService.setDiffs(diffs);
        } catch (final IOException ex) {
            ex.printStackTrace();
            fileHighlightService.setDiffs(null);
        }

        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(e.getProject());
        fileHighlightService.highlightFile(fileEditorManager);

        System.out.println("action=start_code_review status=finished");
    }

    private List<Diff> getPullRequestDiffs(final String repoOwner,
                                           final String repoName,
                                           final String pullRequestId) throws IOException {
        System.out.println(format("action=download_diff status=start repo_owner=%s repo_name=%s pull_request_id=%s",
                repoOwner, repoName, pullRequestId));

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_PR_URL_FORMAT, repoOwner, repoName, pullRequestId));
        request.addHeader("Accept", ACCEPT_V3_DIFF);
        request.addHeader("Authorization", "token " + githubToken);
        request.addHeader("User-Agent", githubUsername);
        final HttpResponse response =  client.execute(request);

        final List<Diff> diffs = new UnifiedDiffParser().parse(response.getEntity().getContent());

        System.out.println(format("action=download_diff status=finished repo_owner=%s repo_name=%s pull_request_id=%s",
                repoOwner, repoName, pullRequestId));

        return diffs;
    }
}
