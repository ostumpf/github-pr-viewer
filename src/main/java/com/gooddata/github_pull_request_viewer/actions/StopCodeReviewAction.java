package com.gooddata.github_pull_request_viewer.actions;

import com.gooddata.github_pull_request_viewer.services.CodeReviewService;
import com.gooddata.github_pull_request_viewer.services.FileHighlightService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;

public class StopCodeReviewAction extends AnAction {

    private static final Logger logger = Logger.getInstance(StopCodeReviewAction.class);

    @Override
    public void update(AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final CodeReviewService codeReviewService =
                ServiceManager.getService(e.getProject(), CodeReviewService.class);

        e.getPresentation().setEnabled(codeReviewService.inProgress());
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        if (e.getProject() == null) {
            logger.warn("action=stop_code_review the project is null, not doing anything");
            return;
        }

        logger.info("action=stop_code_review status=start");

        final CodeReviewService codeReviewService =
                ServiceManager.getService(e.getProject(), CodeReviewService.class);

        codeReviewService.stopCodeReview(e.getProject());

        logger.info("action=stop_code_review status=finished");
    }
}
