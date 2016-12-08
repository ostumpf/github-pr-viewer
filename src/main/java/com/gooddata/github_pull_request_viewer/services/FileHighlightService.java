package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.Comment;
import com.gooddata.github_pull_request_viewer.model.DownloadedComment;
import com.gooddata.github_pull_request_viewer.model.HighlightedRow;
import com.gooddata.github_pull_request_viewer.utils.RegexUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.wickedsource.diffparser.api.model.Diff;
import org.wickedsource.diffparser.api.model.Hunk;
import org.wickedsource.diffparser.api.model.Line;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            final Optional<Diff> diff = getDiff(codeReviewService.getDiffs(), selectedFile.getPath(),
                    fileEditorManager.getProject().getBasePath());
            final List<DownloadedComment> fileComments =
                    getCommentsForFile(codeReviewService.getComments(), selectedFile.getPath(),
                            fileEditorManager.getProject().getBasePath());

            if (diff.isPresent()) {
                highlightDiff(textEditor, codeReviewService, selectedFile, diff.get());
                logger.info("action=highlight_file status=finished");
            } else {
                logger.warn("action=highlight_file no diff for file " + selectedFile.getPath());
            }

            if (!fileComments.isEmpty()) {
                highlightComments(textEditor, fileComments, codeReviewService);
            }
        }
    }

    public void highlightComments(@NotNull final Editor textEditor, final List<DownloadedComment> comments,
                                  final CodeReviewService codeReviewService) {
        MarkupModel markupModel = textEditor.getMarkupModel();


        comments.stream()
                .collect(Collectors.groupingBy(DownloadedComment::getLineNumber,
                        Collectors.reducing(
                                (DownloadedComment c1, DownloadedComment c2) -> new DownloadedComment(
                                        c1.getBody() + "\n\n" + c2.getBody(),
                                        c1.getCommit(), c1.getPath(), c1.getPosition(), c1.getLineNumber()))))
                .values()
                .stream()
                .map(Optional::get)
                .filter(c -> c.getPosition() != null)
                .forEach(c -> {
                    int fileLine = getFileLine(c.getPosition(), c.getPath(), codeReviewService);
                    RangeHighlighter highlighter =
                            markupModel.addLineHighlighter(fileLine, HighlighterLayer.FIRST, null);
                    addGutterIcon(highlighter, c.getBody());
                });
    }

    private int getFileLine(final int position, final String relativePath,
                            final CodeReviewService codeReviewService,
                            final FileEditorManager fileEditorManager) {
        final Optional<Diff> diffOptional = getDiff(codeReviewService.getDiffs(), relativePath, fileEditorManager.getProject().getBasePath());

        if (diffOptional.isPresent()) {
            for (final Hunk hunk : diffOptional.get().getHunks()) {
                int fileLine = hunk.getToFileRange().getLineStart() - 1; // -1 for 0/1 based counting
                int diffLine = 1;

                for (final Line line : hunk.getLines()) {
                    if (line.getLineType().equals(Line.LineType.TO) || line.getLineType().equals(Line.LineType.NEUTRAL)) {
                        fileLine++;
                    }

                    diffLine++;

                    if (diffLine == position) {
                        return fileLine;
                    }
                }
            }

            logger.info("action=get_file_line matching line not found");

            return 1;
        } else {
            logger.warn("action=get_file_line no diff for file " + relativePath);
            return 1;
        }
    }

    private void addGutterIcon(RangeHighlighter rangeHighlighter, final String messageBody) {
        rangeHighlighter.setGutterIconRenderer(new GutterIconRenderer() {
            public Icon getIcon() {
                return IconUtil.getAddIcon();
            }

            public String getTooltipText() {
                return messageBody;
            }

            public boolean isNavigateAction() {
                return false;
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            public AnAction getClickAction() {
                return null;
            }
        });
    }

    private void clearHiglights(final Editor textEditor, final CodeReviewService codeReviewService) {
        final MarkupModel markupModel = textEditor.getMarkupModel();

        codeReviewService.getHighlightedRowsMap().values().stream()
                .flatMap(m -> m.values().stream())
                .map(HighlightedRow::getHighlighter)
                .forEach(markupModel::removeHighlighter);
        codeReviewService.getHighlightedRowsMap().clear();
    }

    private void highlightDiff(final Editor textEditor, final CodeReviewService codeReviewService,
                               final VirtualFile virtualFile, final Diff diff) {
        final String relativePath = codeReviewService.getRelativePath(diff);

        diff.getHunks().forEach(hunk -> {
            int fileLine = hunk.getToFileRange().getLineStart() - 1; // -1 for 0/1 based counting
            int diffLine = 1;

            for (final Line line : hunk.getLines()) {
                if (line.getLineType().equals(Line.LineType.TO)) {
                    highlightRow(textEditor, codeReviewService, virtualFile, relativePath, fileLine, diffLine);
                }

                if (line.getLineType().equals(Line.LineType.TO) || line.getLineType().equals(Line.LineType.NEUTRAL)) {
                    fileLine++;
                }

                diffLine++;
            }
        });
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

    private Optional<Diff> getDiff(final List<Diff> diffs, final String absoluteFilePath,
                                   final String projectBasePath) {
        final String relativeFilePath = getRelativePath(absoluteFilePath, projectBasePath);

        // TODO differentiate between project base path and git root
        return diffs.stream().filter(diff -> diff.getToFileName().endsWith(relativeFilePath)).findFirst();
    }

    private List<DownloadedComment> getCommentsForFile(final List<DownloadedComment> allComments,
                                                       final String absoluteFilePath,
                                                       final String projectBasePath) {
        final String relativeFilePath = getRelativePath(absoluteFilePath, projectBasePath);

        return allComments.stream().filter(c -> relativeFilePath.endsWith(c.getPath())).collect(Collectors.toList());
    }

    private String getRelativePath(final String absoluteFilePath, final String projectBasePath) {
        return new File(projectBasePath)
                .toURI()
                .relativize(new File(absoluteFilePath).toURI())
                .toString();
    }
}
