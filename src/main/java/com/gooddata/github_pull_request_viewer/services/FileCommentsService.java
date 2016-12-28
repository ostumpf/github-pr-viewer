/*
 * Copyright (C) 2007-2016, GoodData(R) Corporation. All rights reserved.
 */
package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.diff_parser.Diff;
import com.gooddata.github_pull_request_viewer.model.DownloadedComment;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileCommentsService {
    private static final Logger logger = Logger.getInstance(FileHighlightService.class);

    public void showComments(@NotNull final FileEditorManager fileEditorManager) {
        logger.info("action=show_comments status=start");

        final VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        if (selectedFiles.length != 1) {
            logger.warn("action=show_comments Unexpected number of selected files: " + selectedFiles.length);
            return;
        }

        final CodeReviewService codeReviewService =
                ServiceManager.getService(fileEditorManager.getProject(), CodeReviewService.class);

        final Editor textEditor = fileEditorManager.getSelectedTextEditor();
        final VirtualFile selectedFile = selectedFiles[0];

        if (!codeReviewService.inProgress()) {
            clearHiglights(textEditor, codeReviewService);
            logger.info("action=show_comments comments removed");
        } else {
            final List<DownloadedComment> fileComments =
                    codeReviewService.getCommentsForFile(fileEditorManager.getProject(), selectedFile);

            if (!fileComments.isEmpty()) {
                showComments(textEditor, fileComments, codeReviewService);
                logger.info("action=show_comments status=finished");
            } else {
                logger.info("action=show_comments status=finished no comments to display");
            }
        }
    }

    private void clearHiglights(final Editor textEditor, final CodeReviewService codeReviewService) {
        final MarkupModel markupModel = textEditor.getMarkupModel();

        markupModel.removeAllHighlighters();
        codeReviewService.getHighlightedRowsMap().clear();
    }

    private void showComments(final Editor textEditor,
                              final List<DownloadedComment> comments,
                              final CodeReviewService codeReviewService) {
        comments.stream()
                .filter(c -> c.getPosition() != null && c.getPosition() > 0)
                .sorted()
                .collect(Collectors.groupingBy(DownloadedComment::getPosition))
                .forEach((Integer diffLine, List<DownloadedComment> commentsOnLine) -> {
                            final String relativeFilePath = commentsOnLine.get(0).getPath();
                            final Optional<Diff> diffOptional = codeReviewService.getDiff(relativeFilePath);

                            if (diffOptional.isPresent()) {
                                final int fileLine = diffOptional.get().getFileLine(diffLine);
                                showGutterIconOnLine(commentsOnLine, fileLine, textEditor);
                            } else {
                                logger.warn("action=show_comments cannot find diff for file " + relativeFilePath);
                            }
                        }
                );
    }

    private void showGutterIconOnLine(final List<DownloadedComment> commentList, final int fileLine,
                                      final Editor textEditor) {
        final MarkupModel markupModel = textEditor.getMarkupModel();
        commentList
                .stream()
                .map(DownloadedComment::getDisplayText)
                .reduce((msg1, msg2) -> msg1 + "\n\n" + msg2)
                .ifPresent(text -> {
                    final GutterIconRenderer gutterIconRenderer = createGutterIconRenderer(text);

                    if (!commentGutterIconExists(markupModel, gutterIconRenderer)) {
                        final RangeHighlighter highlighter =
                                markupModel.addLineHighlighter(fileLine, HighlighterLayer.FIRST, null);
                        highlighter.setGutterIconRenderer(gutterIconRenderer);
                    }
                });
    }

    private boolean commentGutterIconExists(final MarkupModel markupModel, final GutterIconRenderer iconRenderer) {
        final RangeHighlighter[] allHighLighters = markupModel.getAllHighlighters();

        return Arrays.stream(allHighLighters)
                .anyMatch(highlighter -> iconRenderer.equals(highlighter.getGutterIconRenderer()));
    }

    private GutterIconRenderer createGutterIconRenderer(final String messageBody) {
        return new GutterIconRenderer() {
            private final String tooltipText = messageBody;

            public Icon getIcon() {
                return IconUtil.getAddIcon();
            }

            public String getTooltipText() {
                return tooltipText;
            }

            public boolean isNavigateAction() {
                return false;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }

                final GutterIconRenderer renderer2 = (GutterIconRenderer) obj;
                return tooltipText.equals(renderer2.getTooltipText());
            }

            @Override
            public int hashCode() {
                return this.tooltipText.hashCode();
            }

            public AnAction getClickAction() {
                return null;
            }
        };
    }
}
