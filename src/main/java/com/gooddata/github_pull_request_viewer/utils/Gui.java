package com.gooddata.github_pull_request_viewer.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.UIUtil;

import static com.gooddata.github_pull_request_viewer.utils.UrlUtils.GITHUB_URL_PATTERN;

public class Gui {

    public static String getGitHubPullRequestUrl(final Project project) {
        return Messages.showInputDialog(project, "GitHub Pull Request URL", "Pull Request",
                UIUtil.getQuestionIcon(), "https://github.com/<repo_owner>/<repo_name>/pull/<pr_id>",
                new InputValidator() {
                    @Override
                    public boolean checkInput(final String s) {
                        return GITHUB_URL_PATTERN.matcher(s).matches();
                    }

                    @Override
                    public boolean canClose(final String s) {
                        return checkInput(s);
                    }
                });
    }

}
