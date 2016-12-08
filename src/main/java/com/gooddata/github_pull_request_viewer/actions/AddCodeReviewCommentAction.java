package com.gooddata.github_pull_request_viewer.actions;

import com.gooddata.github_pull_request_viewer.model.Comment;
import com.gooddata.github_pull_request_viewer.model.HighlightedRow;
import com.gooddata.github_pull_request_viewer.services.CodeReviewService;
import com.gooddata.github_pull_request_viewer.services.GitHubRestService;
import com.gooddata.github_pull_request_viewer.utils.Gui;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;

public class AddCodeReviewCommentAction extends AnAction {

    private static final Logger logger = Logger.getInstance(AddCodeReviewCommentAction.class);

    @Override
    public void update(AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final CodeReviewService codeReviewService =
                ServiceManager.getService(e.getProject(), CodeReviewService.class);

        final int selectedLine = getSelectedLine(e.getProject());
        final VirtualFile selectedFile = getSelectedFile(e.getProject());

        e.getPresentation().setEnabled(codeReviewService.inProgress() &&
                selectedFile != null &&
                codeReviewService.getHighlightedRowsMap().containsKey(selectedFile) &&
                codeReviewService.getHighlightedRowsMap().get(selectedFile).containsKey(selectedLine));
    }

    @Override
    public void actionPerformed(final AnActionEvent e) {
        if (e.getProject() == null) {
            logger.warn("action=add_comment the project is null, not doing anything");
            return;
        }

        logger.info("action=add_comment status=start");

        final CodeReviewService codeReviewService =
                ServiceManager.getService(e.getProject(), CodeReviewService.class);
        final GitHubRestService gitHubRestService =
                ServiceManager.getService(e.getProject(), GitHubRestService.class);

        final int selectedLine = getSelectedLine(e.getProject());
        final VirtualFile selectedFile = getSelectedFile(e.getProject());
        final HighlightedRow highlightedRow = codeReviewService.getHighlightedRowsMap().get(selectedFile).get(selectedLine);

        final String comment = Gui.getCommentText(e.getProject());
        if (comment == null) {
            logger.info("action=add_comment status=finished User cancelled");
            return;
        }

        try {
            final String commit = gitHubRestService.getLastCommit(e.getProject());

            gitHubRestService.postComment(e.getProject(),
                    new Comment(comment, commit,
                            highlightedRow.getRelativeFilePath(),
                            highlightedRow.getDiffRowNumber())
            );

            Notifications.Bus.notify(new Notification("githubPrRequestViewer", "Success", "The comment has been posted",
                    NotificationType.INFORMATION), e.getProject());
        } catch (IOException ex) {
            Messages.showErrorDialog(e.getProject(), ex.getMessage(), "Error");
        }

        logger.info("action=add_comment status=finished");
    }

    private VirtualFile getSelectedFile(final Project project) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

        final VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        if (selectedFiles.length != 1) {
            logger.warn("action=highlight_file Unexpected number of selected files: " + selectedFiles.length);
            return null;
        }

        return selectedFiles[0];
    }

    private int getSelectedLine(final Project project) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final Editor editor = fileEditorManager.getSelectedTextEditor();
        final SelectionModel selectionModel = editor.getSelectionModel();
        return selectionModel.getSelectionStartPosition().getLine();
    }
}
