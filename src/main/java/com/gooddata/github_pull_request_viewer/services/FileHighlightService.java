package com.gooddata.github_pull_request_viewer.services;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.wickedsource.diffparser.api.model.Diff;
import org.wickedsource.diffparser.api.model.Line;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class FileHighlightService {

    private static final Logger logger = Logger.getInstance(FileHighlightService.class);
    private static final Color GREEN = new Color(234, 255, 234);
    private final TextAttributes backgroundTextAttributes;

    private List<Diff> diffs;

    public FileHighlightService() {
        backgroundTextAttributes = new TextAttributes();
        backgroundTextAttributes.setBackgroundColor(GREEN);
    }

    public void setDiffs(final List<Diff> diffs) {
        this.diffs = diffs;
    }

    public void highlightFile(@NotNull final FileEditorManager fileEditorManager) {
        logger.info("action=highlight_file status=start");

        final VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        if (selectedFiles.length != 1) {
            logger.warn("action=highlight_file Unexpected number of selected files: " + selectedFiles.length);
            return;
        }

        if (diffs == null) {
            logger.warn("action=highlight_file No diffs");
            return;
        }

        final VirtualFile selectedFile = selectedFiles[0];
        final Optional<Diff> diff = getDiff(selectedFile.getPath(), fileEditorManager.getProject().getBasePath());

        if (!diff.isPresent()) {
            logger.warn("action=highlight_file no diff for file " + selectedFile.getPath());
            return;
        }

        final Editor textEditor = fileEditorManager.getSelectedTextEditor();
        final MarkupModel markupModel = textEditor.getMarkupModel();

        diff.get().getHunks().forEach(hunk -> {
            int lineIndex = hunk.getToFileRange().getLineStart() - 1; // -1 for 0/1 based counting

            for (final Line line : hunk.getLines()) {
                if (line.getLineType().equals(Line.LineType.TO)) {
                    markupModel.addLineHighlighter(lineIndex, HighlighterLayer.WARNING + 1, backgroundTextAttributes);
                }

                if (line.getLineType().equals(Line.LineType.TO) || line.getLineType().equals(Line.LineType.NEUTRAL)) {
                    lineIndex++;
                }
            }
        });
        logger.info("action=highlight_file status=finished");
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
