package com.gooddata.github_pull_request_viewer.utils;

import com.gooddata.github_pull_request_viewer.model.PullRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {

    public static final Pattern GITHUB_URL_PATTERN = Pattern.compile("https://github.com/([^/]+)/([^/]+)/pull/([0-9]+)/?");
    private static final Pattern DIFF_FILES_PATTERN = Pattern.compile("diff\\s+--git\\s+(\\S+)\\s+(\\S+)");

    public static PullRequest getPullRequest(final String pullRequestUrl) {
        final Matcher matcher = GITHUB_URL_PATTERN.matcher(pullRequestUrl);

        if (matcher.find()) {
            return new PullRequest(matcher.group(1), matcher.group(2), matcher.group(3));
        } else {
            throw new IllegalStateException("The url " + pullRequestUrl + " does not match GitHub pull request URL");
        }
    }

    public static String getRelativeFilePath(final String headerLine) {
        final Matcher matcher = DIFF_FILES_PATTERN.matcher(headerLine);

        if (matcher.find()) {
            return matcher.group(1).substring(2); // skip a/...
        } else {
            throw new IllegalStateException("The header line " + headerLine + " does not match expected pattern");
        }
    }
}
