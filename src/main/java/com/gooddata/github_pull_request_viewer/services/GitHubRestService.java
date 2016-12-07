package com.gooddata.github_pull_request_viewer.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.gooddata.github_pull_request_viewer.model.PullRequestSource;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;

public class GitHubRestService {

    private static final String GITHUB_API_PR_URL_FORMAT = "https://api.github.com/repos/%s/%s/pulls/%s";
    private static final String ACCEPT_V3_DIFF = "application/vnd.github.v3.diff";
    private static final String ACCEPT_V3 = "application/vnd.github.v3";
    private static final String USER_AGENT = "gooddata";

    private static final Logger logger = Logger.getInstance(GitHubRestService.class);

    public InputStream getPullRequestDiff(final Project project, final String token) throws IOException {
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);

        final PullRequest pullRequest = codeReviewService.getPullRequest();

        logger.info(format("action=download_diff status=start repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_PR_URL_FORMAT, pullRequest.getRepoOwner(),
                pullRequest.getRepoName(), pullRequest.getPullRequestId()));
        request.addHeader("Accept", ACCEPT_V3_DIFF);
        request.addHeader("Authorization", "token " + token);
        request.addHeader("User-Agent", USER_AGENT);
        final HttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException("Cannot download the pull request diff from GitHub: " +
                    response.getStatusLine().toString());
        }

        logger.info(format("action=download_diff status=finished repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        return response.getEntity().getContent();
    }

    public void postComment(final String comment, final String commit, final String relativeFilePath, final int diffRowNumber) {

    }

    public PullRequestSource getPullRequestSource(final Project project, final String token) throws IOException {
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);

        final PullRequest pullRequest = codeReviewService.getPullRequest();

        logger.info(format("action=download_pull_request status=start repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_PR_URL_FORMAT, pullRequest.getRepoOwner(),
                pullRequest.getRepoName(), pullRequest.getPullRequestId()));
        request.addHeader("Accept", ACCEPT_V3);
        request.addHeader("Authorization", "token " + token);
        request.addHeader("User-Agent", USER_AGENT);
        final HttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException("Cannot download the pull request diff from GitHub: " +
                    response.getStatusLine().toString());
        }

        final PullRequestSource pullRequestSource = parsePullRequestSource(pullRequest, response.getEntity().getContent());

        logger.info(format("action=download_pull_request status=finished repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        return pullRequestSource;
    }

    private PullRequestSource parsePullRequestSource(final PullRequest pullRequest, final InputStream content) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();

        final JsonNode rootNode = objectMapper.readTree(content);
        final JsonNode headNode = rootNode.path("head");
        final String label = headNode.path("label").textValue();
        final String[] pair = label.split(":");

        return new PullRequestSource(pullRequest.getRepoName(), pair[0], pair[1]);
    }
}
