package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.diff_parser.Diff;
import com.gooddata.github_pull_request_viewer.diff_parser.Hunk;
import com.gooddata.github_pull_request_viewer.diff_parser.Line;
import com.gooddata.github_pull_request_viewer.model.HighlightedRow;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Optional;

public class FileHighlightService {
    private static final Logger logger = Logger.getInstance(FileHighlightService.class);
    private static final Color GREEN = new Color(234, 255, 234);
    private final TextAttributes backgroundTextAttributes = new TextAttributes();

    public FileHighlightService() {
        backgroundTextAttributes.setBackgroundColor(GREEN);
    }

    public void highlightFile(@NotNull final FileEditorManager fileEditorManager) {
        logger.info("action=highlight_file status=start");

        final VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        if (selectedFiles.length != 1) {
            logger.warn("action=highlight_file Unexpected number of selected files: " + selectedFiles.length);
            return;
        }

        final CodeReviewService codeReviewService =
                ServiceManager.getService(fileEditorManager.getProject(), CodeReviewService.class);

        final Editor textEditor = fileEditorManager.getSelectedTextEditor();
        final VirtualFile selectedFile = selectedFiles[0];

        if (!codeReviewService.inProgress()) {
            clearHiglights(textEditor, codeReviewService);
            logger.info("action=highlight_file highlights removed");
        } else {
            final Optional<Diff> diff = codeReviewService.getDiff(fileEditorManager.getProject(), selectedFile);

            if (diff.isPresent()) {
                highlightDiff(textEditor, codeReviewService, selectedFile, diff.get());
                logger.info("action=highlight_file status=finished");
            } else {
                logger.warn("action=highlight_file no diff for file " + selectedFile.getPath());
            }
        }
    }

    private void clearHiglights(final Editor textEditor, final CodeReviewService codeReviewService) {
        final MarkupModel markupModel = textEditor.getMarkupModel();

        markupModel.removeAllHighlighters();
        codeReviewService.getHighlightedRowsMap().clear();
    }

    private void highlightDiff(final Editor textEditor, final CodeReviewService codeReviewService,
                               final VirtualFile virtualFile, final Diff diff) {
        int diffLine = 1;

        for (final Hunk hunk : diff.getHunks()) {
            int fileLine = hunk.getTargetStart() - 1; // -1 for 0/1 based counting

            for (final Line line : hunk.getLines()) {
                if (line.getType().equals(Line.Type.ADDED)) {
                    highlightRow(textEditor, codeReviewService, virtualFile, diff.getRelativePath(), fileLine, diffLine);
                }

                if (line.getType().equals(Line.Type.ADDED) || line.getType().equals(Line.Type.NEUTRAL)) {
                    fileLine++;
                }

                diffLine++;
            }
        }
    }

    private void highlightRow(final Editor textEditor,
                              final CodeReviewService codeReviewService,
                              final VirtualFile virtualFile,
                              final String relativePath,
                              final int fileLine,
                              final int diffLine) {
        final MarkupModel markupModel = textEditor.getMarkupModel();
        final RangeHighlighter highlighter =
                markupModel.addLineHighlighter(fileLine, HighlighterLayer.WARNING + 1, backgroundTextAttributes);

        final HighlightedRow highlightedRow = new HighlightedRow(fileLine, diffLine, relativePath, highlighter);

        codeReviewService.getHighlightedRowsMap().putIfAbsent(virtualFile, new HashMap<>());
        codeReviewService.getHighlightedRowsMap().get(virtualFile).put(fileLine, highlightedRow);
    }
}
