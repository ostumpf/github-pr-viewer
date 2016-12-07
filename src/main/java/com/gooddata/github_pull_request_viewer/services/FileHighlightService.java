package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.model.HighlightedRow;
import com.gooddata.github_pull_request_viewer.utils.RegexUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.wickedsource.diffparser.api.model.Diff;
import org.wickedsource.diffparser.api.model.Line;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FileHighlightService {

    private static final Logger logger = Logger.getInstance(FileHighlightService.class);
    private static final Color GREEN = new Color(234, 255, 234);
    private final TextAttributes backgroundTextAttributes = new TextAttributes();
    private final Map<VirtualFile, Map<Integer, HighlightedRow>> highlightedRowsMap = new HashMap<>();

    private List<Diff> diffs;

    public FileHighlightService() {
        backgroundTextAttributes.setBackgroundColor(GREEN);
    }

    public void setDiffs(final List<Diff> diffs) {
        this.diffs = diffs;
    }

    public List<Diff> getDiffs() {
        return diffs;
    }

    public void highlightFile(@NotNull final FileEditorManager fileEditorManager) {
        logger.info("action=highlight_file status=start");

        final VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        if (selectedFiles.length != 1) {
            logger.warn("action=highlight_file Unexpected number of selected files: " + selectedFiles.length);
            return;
        }

        final Editor textEditor = fileEditorManager.getSelectedTextEditor();
        final MarkupModel markupModel = textEditor.getMarkupModel();
        final VirtualFile selectedFile = selectedFiles[0];

        if (diffs == null) {
            clearHiglights(markupModel);
            logger.info("action=highlight_file highlights removed");
        } else if (!highlightedRowsMap.containsKey(selectedFile)) {
            final Optional<Diff> diff = getDiff(selectedFile.getPath(), fileEditorManager.getProject().getBasePath());

            if (diff.isPresent()) {
                highlightDiff(markupModel, selectedFile, diff.get());
                logger.info("action=highlight_file status=finished");
            } else {
                logger.warn("action=highlight_file no diff for file " + selectedFile.getPath());
            }
        } else {
            logger.info("action=highlight_file highlighting already done, skipping");
        }
    }

    private void clearHiglights(final MarkupModel markupModel) {
        highlightedRowsMap.values().stream()
                .flatMap(m -> m.values().stream())
                .map(HighlightedRow::getHighlighter)
                .forEach(markupModel::removeHighlighter);
        highlightedRowsMap.clear();
    }

    private void highlightDiff(final MarkupModel markupModel, final VirtualFile virtualFile, final Diff diff) {
        final String targetCommit = getTargetCommit(diff);

        diff.getHunks().forEach(hunk -> {
            int fileLine = hunk.getToFileRange().getLineStart() - 1; // -1 for 0/1 based counting
            int diffLine = 1;

            for (final Line line : hunk.getLines()) {
                if (line.getLineType().equals(Line.LineType.TO)) {
                    highlightRow(markupModel, virtualFile, fileLine, diffLine, targetCommit);
                }

                if (line.getLineType().equals(Line.LineType.TO) || line.getLineType().equals(Line.LineType.NEUTRAL)) {
                    fileLine++;
                }

                diffLine++;
            }
        });
    }

    private String getTargetCommit(final Diff diff) {
        final List<String> headers = diff.getHeaderLines();
        if (headers.size() < 2) {
            logger.warn("action=get_target_commit cannot determine target commit for diff");
            return null;
        }

        return RegexUtils.getTargetCommit(headers.get(1));
    }

    private void highlightRow(final MarkupModel markupModel, final VirtualFile virtualFile, final int fileLine,
                              final int diffLine, final String targetCommit) {
        final RangeHighlighter highlighter =
                markupModel.addLineHighlighter(fileLine, HighlighterLayer.WARNING + 1, backgroundTextAttributes);

        final HighlightedRow highlightedRow = new HighlightedRow(fileLine, diffLine, targetCommit, highlighter);

        highlightedRowsMap.putIfAbsent(virtualFile, new HashMap<>());
        highlightedRowsMap.get(virtualFile).putIfAbsent(fileLine, highlightedRow);
    }

    private Optional<Diff> getDiff(final String absoluteFilePath, final String projectBasePath) {
        final String relativeFilePath = new File(projectBasePath)
                .toURI()
                .relativize(new File(absoluteFilePath).toURI())
                .toString();

        // TODO differentiate between project base path and git root
        return diffs.stream().filter(diff -> diff.getToFileName().endsWith(relativeFilePath)).findFirst();
    }
}
