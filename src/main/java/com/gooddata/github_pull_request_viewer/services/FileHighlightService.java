package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.DownloadedComment;
import com.gooddata.github_pull_request_viewer.model.HighlightedRow;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class FileHighlightService {
    private static final DateFormat commentDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
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
        comments.stream()
                .filter(c -> c.getPosition() != null)
                .sorted()
                .collect(Collectors.groupingBy(DownloadedComment::getPosition))
                .forEach((Integer position, List<DownloadedComment> commentList) -> {
                            showGutterIconOnLine(commentList,
                                    getFileLine(position, commentList.get(0).getPath(), codeReviewService),
                                    textEditor);
                        }
                );
    }


    private void showGutterIconOnLine(final List<DownloadedComment> commentList, final int line,
                                      final Editor textEditor) {
        final MarkupModel markupModel = textEditor.getMarkupModel();
        commentList
                .stream()
                .map(c -> formatCommentMessage(c))
                .reduce((msg1, msg2) -> msg1 + "\n\n" + msg2)
                .ifPresent(text -> {
                    final GutterIconRenderer gutterIconRenderer = getGutterIconRenderer(text);

                    if (!commentGutterIconExists(markupModel, gutterIconRenderer)) {
                        RangeHighlighter highlighter =
                                markupModel.addLineHighlighter(line, HighlighterLayer.FIRST, null);
                        highlighter.setGutterIconRenderer(gutterIconRenderer);
                    }
                });
    }

    private boolean commentGutterIconExists(final MarkupModel markupModel, final GutterIconRenderer iconRenderer) {
        final RangeHighlighter[] allHighLighters = markupModel.getAllHighlighters();

        return asList(allHighLighters).stream()
                .anyMatch(highlighter -> iconRenderer.equals(highlighter.getGutterIconRenderer()));
    }

    private String formatCommentMessage(final DownloadedComment c1) {
        return new StringBuilder().append("[")
                .append(commentDateFormat.format(c1.getUpdatedAt()))
                .append(" ").append(c1.getUser().getUsername())
                .append("]: ")
                .append(c1.getBody())
                .toString();
    }

    private int getFileLine(final int position, final String relativePath,
                            final CodeReviewService codeReviewService) {
        final Optional<Diff> diffOptional = getDiff(codeReviewService.getDiffs(), relativePath);
        int diffLine = 1;

        if (diffOptional.isPresent()) {
            for (final Hunk hunk : diffOptional.get().getHunks()) {
                int fileLine = hunk.getToFileRange().getLineStart() - 1; // -1 for 0/1 based counting

                for (final Line line : hunk.getLines()) {
                    if (line.getLineType().equals(Line.LineType.TO) ||
                            line.getLineType().equals(Line.LineType.NEUTRAL)) {
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

    private GutterIconRenderer getGutterIconRenderer(final String messageBody) {
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

                GutterIconRenderer renderer2 = (GutterIconRenderer) obj;
                if (tooltipText.equals(renderer2.getTooltipText())) {
                    return true;
                } else {
                    return false;
                }
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

    private void addGutterIcon(RangeHighlighter rangeHighlighter, final String messageBody) {
        final GutterIconRenderer gutterIconRenderer = getGutterIconRenderer(messageBody);
        if (!gutterIconRenderer.equals(rangeHighlighter.getGutterIconRenderer())) {
            rangeHighlighter.setGutterIconRenderer(gutterIconRenderer);
        }
    }

    private void clearHiglights(final Editor textEditor, final CodeReviewService codeReviewService) {
        final MarkupModel markupModel = textEditor.getMarkupModel();

        markupModel.removeAllHighlighters();
        codeReviewService.getHighlightedRowsMap().clear();
    }

    private void highlightDiff(final Editor textEditor, final CodeReviewService codeReviewService,
                               final VirtualFile virtualFile, final Diff diff) {
        final String relativePath = codeReviewService.getRelativePath(diff);
        int diffLine = 1;

        for (final Hunk hunk : diff.getHunks()) {
            int fileLine = hunk.getToFileRange().getLineStart() - 1; // -1 for 0/1 based counting

            for (final Line line : hunk.getLines()) {
                if (line.getLineType().equals(Line.LineType.TO)) {
                    highlightRow(textEditor, codeReviewService, virtualFile, relativePath, fileLine, diffLine);
                }

                if (line.getLineType().equals(Line.LineType.TO) || line.getLineType().equals(Line.LineType.NEUTRAL)) {
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

    private Optional<Diff> getDiff(final List<Diff> diffs, final String absoluteFilePath,
                                   final String projectBasePath) {
        final String relativeFilePath = getRelativePath(absoluteFilePath, projectBasePath);

        return getDiff(diffs, relativeFilePath);
    }

    private Optional<Diff> getDiff(final List<Diff> diffs, final String relativeFilePath) {
        // TODO differentiate between project base path and git root
        return diffs.stream().filter(diff -> diff.getToFileName().endsWith(relativeFilePath)).findFirst();
    }

    private List<DownloadedComment> getCommentsForFile(final List<DownloadedComment> allComments,
                                                       final String absoluteFilePath,
                                                       final String projectBasePath) {
        final String relativeFilePath = getRelativePath(absoluteFilePath, projectBasePath);

        return allComments.stream().filter(c -> c.getPath().endsWith(relativeFilePath)).collect(Collectors.toList());
    }

    private String getRelativePath(final String absoluteFilePath, final String projectBasePath) {
        return new File(projectBasePath)
                .toURI()
                .relativize(new File(absoluteFilePath).toURI())
                .toString();
    }
}
