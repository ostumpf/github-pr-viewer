package com.gooddata.github_pull_request_viewer.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gooddata.github_pull_request_viewer.model.Comment;
import com.gooddata.github_pull_request_viewer.model.DownloadedComment;
import com.gooddata.github_pull_request_viewer.model.PullRequest;
import com.gooddata.github_pull_request_viewer.model.PullRequestSource;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import static java.lang.String.format;

public class GitHubRestService {

    private static final String GITHUB_API_PR_URL_FORMAT = "https://api.github.com/repos/%s/%s/pulls/%s";
    private static final String GITHUB_API_COMMENT_URL_FORMAT = GITHUB_API_PR_URL_FORMAT + "/comments";
    private static final String GITHUB_API_COMMITS_URL_FORMAT = GITHUB_API_PR_URL_FORMAT + "/commits";
    private static final String ACCEPT_V3_DIFF = "application/vnd.github.v3.diff";
    private static final String ACCEPT_V3 = "application/vnd.github.v3";
    private static final String USER_AGENT = "gooddata";

    private static final Logger logger = Logger.getInstance(GitHubRestService.class);

    public InputStream getPullRequestDiff(final Project project) throws IOException {
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);

        final PullRequest pullRequest = codeReviewService.getPullRequest();

        logger.info(format("action=download_diff status=start repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_PR_URL_FORMAT, pullRequest.getRepoOwner(),
                pullRequest.getRepoName(), pullRequest.getPullRequestId()));
        request.addHeader("Accept", ACCEPT_V3_DIFF);
        request.addHeader("Authorization", codeReviewService.getGithubAuthorization());
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

    public void postComment(final Project project, final Comment comment) throws IOException {
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);

        final PullRequest pullRequest = codeReviewService.getPullRequest();
        final String json = new ObjectMapper().writeValueAsString(comment);

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpPost request = new HttpPost(format(GITHUB_API_COMMENT_URL_FORMAT, pullRequest.getRepoOwner(),
                pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        request.addHeader("Authorization", codeReviewService.getGithubAuthorization());
        request.addHeader("User-Agent", USER_AGENT);
        request.setEntity(new StringEntity(json));
        final HttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != 201) {
            throw new IllegalStateException("Cannot post the comment: " + response.getStatusLine().toString());
        }
    }

    public List<DownloadedComment> getComments(final Project project) throws IOException {
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);

        final PullRequest pullRequest = codeReviewService.getPullRequest();

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_COMMENT_URL_FORMAT, pullRequest.getRepoOwner(),
                pullRequest.getRepoName(), pullRequest.getPullRequestId()));
        request.addHeader("Accept", ACCEPT_V3_DIFF);
        request.addHeader("Authorization", codeReviewService.getGithubAuthorization());
        request.addHeader("User-Agent", USER_AGENT);

        final HttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException("Cannot download comments for pull request from GitHub: " +
                    response.getStatusLine().toString());
        }

        return parseComments(response.getEntity().getContent());

    }

    public PullRequestSource getPullRequestSource(final Project project) throws IOException {
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);

        final PullRequest pullRequest = codeReviewService.getPullRequest();

        logger.info(format("action=download_pull_request status=start repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_PR_URL_FORMAT, pullRequest.getRepoOwner(),
                pullRequest.getRepoName(), pullRequest.getPullRequestId()));
        request.addHeader("Accept", ACCEPT_V3);
        request.addHeader("Authorization", codeReviewService.getGithubAuthorization());
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

    public String getLastCommit(final Project project) throws IOException {
        final CodeReviewService codeReviewService =
                ServiceManager.getService(project, CodeReviewService.class);

        final PullRequest pullRequest = codeReviewService.getPullRequest();

        logger.info(format("action=get_last_commit status=start repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        final HttpClient client = HttpClientBuilder.create().build();
        final HttpGet request = new HttpGet(format(GITHUB_API_COMMITS_URL_FORMAT, pullRequest.getRepoOwner(),
                pullRequest.getRepoName(), pullRequest.getPullRequestId()));
        request.addHeader("Accept", ACCEPT_V3);
        request.addHeader("Authorization", codeReviewService.getGithubAuthorization());
        request.addHeader("User-Agent", USER_AGENT);
        final HttpResponse response = client.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IllegalStateException("Cannot download the pull request diff from GitHub: " +
                    response.getStatusLine().toString());
        }

        final String lastCommit = parseLastCommit(response.getEntity().getContent());

        logger.info(format("action=get_last_commit status=finished repo_owner=%s repo_name=%s pull_request_id=%s",
                pullRequest.getRepoOwner(), pullRequest.getRepoName(), pullRequest.getPullRequestId()));

        return lastCommit;
    }

    private String parseLastCommit(final InputStream content) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();

        final JsonNode rootNode = objectMapper.readTree(content);
        final Iterator<JsonNode> iterator = rootNode.elements();
        JsonNode lastNode = iterator.next();

        while (iterator.hasNext()) {
            lastNode = iterator.next();
        }

        return lastNode.path("sha").textValue();
    }

    private List<DownloadedComment> parseComments(final InputStream inputStream) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final StringWriter stringWriter = new StringWriter();
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.setDateFormat(df);

        IOUtils.copy(inputStream, stringWriter, Charset.defaultCharset());
        return objectMapper.readValue(stringWriter.toString(), new TypeReference<List<DownloadedComment>>() {});
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
