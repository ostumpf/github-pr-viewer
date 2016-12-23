package com.gooddata.github_pull_request_viewer.actions;

import com.gooddata.github_pull_request_viewer.services.CodeReviewService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.actions.BasicAction;
import git4idea.branch.GitBranchUtil;

import java.io.File;

public class OpenCodeReviewFilesAction extends AnAction {

    private static final Logger logger = Logger.getInstance(OpenCodeReviewFilesAction.class);

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
            logger.warn("action=open_code_review_files the project is null, not doing anything");
            return;
        }

        logger.info("action=open_code_review_files status=start");

        final CodeReviewService codeReviewService =
                ServiceManager.getService(e.getProject(), CodeReviewService.class);

        ApplicationManager.getApplication().invokeLater(BasicAction::saveAll);

        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(e.getProject());
        final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

        codeReviewService.getDiffs().forEach(diff -> {
            final VirtualFile projectRoot = GitBranchUtil.getCurrentRepository(e.getProject()).getRoot();

            final VirtualFile virtualFile = localFileSystem.findFileByIoFile(new File(projectRoot.getPath(), diff.getRelativePath()));
            fileEditorManager.openFile(virtualFile, false, true);
        });

        logger.info("action=open_code_review_files status=finished");
    }
}
