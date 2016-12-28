package com.gooddata.github_pull_request_viewer;

import com.gooddata.github_pull_request_viewer.services.FileCommentsService;
import com.gooddata.github_pull_request_viewer.services.FileHighlightService;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

public class PluginStartupActivity implements StartupActivity {

    private static final Logger logger = Logger.getInstance(PluginStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        logger.info("action=plugin_startup status=start");

        final MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorListener());

        logger.info("action=plugin_startup status=finished");
    }

    private class FileEditorListener implements FileEditorManagerListener {

        @Override
        public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {
            logger.info("action=file_editor_file_opened");
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {
            logger.info("action=file_editor_file_closed");
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent fileEditorManagerEvent) {
            logger.info("action=file_editor_selection_changed");

            final FileHighlightService fileHighlightService =
                    ServiceManager.getService(fileEditorManagerEvent.getManager().getProject(),
                            FileHighlightService.class);

            final FileCommentsService fileCommentsService =
                    ServiceManager.getService(fileEditorManagerEvent.getManager().getProject(),
                            FileCommentsService.class);

            fileCommentsService.showComments(fileEditorManagerEvent.getManager());
            fileHighlightService.highlightFile(fileEditorManagerEvent.getManager());
        }
    }
}
