package com.gooddata.github_pull_request_viewer.utils;

import com.gooddata.github_pull_request_viewer.model.PullRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    public static final Pattern GITHUB_URL_PATTERN = Pattern.compile("https://github.com/([^/]+)/([^/]+)/pull/([0-9]+)/?");

    public static PullRequest getPullRequest(final String pullRequestUrl) {
        final Matcher matcher = GITHUB_URL_PATTERN.matcher(pullRequestUrl);

        if (matcher.find()) {
            return new PullRequest(matcher.group(1), matcher.group(2), matcher.group(3));
        } else {
            throw new IllegalStateException("The url " + pullRequestUrl + " does not match GitHub pull request URL");
        }
    }
}
