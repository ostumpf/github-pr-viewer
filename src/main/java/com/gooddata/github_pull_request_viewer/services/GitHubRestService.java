package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.gooddata.github_pull_request_viewer.model.PullRequestSource;
import com.intellij.openapi.diagnostic.Logger;
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
    private static final String USER_AGENT = "gooddata";

    private static final Logger logger = Logger.getInstance(GitHubRestService.class);

    public InputStream getPullRequestDiff(final PullRequest pullRequest, final String token) throws IOException {
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

    public void postComment() {

    }

    public PullRequestSource getPullRequestSource(final String pullRequestUrl) {
        return null;
    }
}
