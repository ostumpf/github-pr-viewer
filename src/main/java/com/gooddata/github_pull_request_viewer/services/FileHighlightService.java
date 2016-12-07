package com.gooddata.github_pull_request_viewer.services;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.wickedsource.diffparser.api.model.Diff;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class FileHighlightService {

    private List<Diff> diffs;
    private final TextAttributes backgroundTextAttributes;

    public FileHighlightService() {
        backgroundTextAttributes = new TextAttributes();
        backgroundTextAttributes.setBackgroundColor(JBColor.orange);
    }

    public void setDiffs(final List<Diff> diffs) {
        this.diffs = diffs;
    }

    public void highlightFile(@NotNull final FileEditorManager fileEditorManager) {
        System.out.println("action=highlight_file status=start");

        final VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        if (selectedFiles.length != 1) {
            System.out.println("action=highlight_file Unexpected number of selected files: " + selectedFiles.length);
            return;
        }

        if (diffs == null) {
            System.out.println("action=highlight_file No diffs");
            return;
        }

        final VirtualFile selectedFile = selectedFiles[0];
        final Optional<Diff> diff = getDiff(selectedFile.getPath(), fileEditorManager.getProject().getBasePath());

        if (!diff.isPresent()) {
            System.out.println("action=highlight_file no diff for file " + selectedFile.getPath());
            return;
        }

        final Editor textEditor = fileEditorManager.getSelectedTextEditor();

        diff.get().getHunks().forEach(hunk -> {
            final int startIndex = hunk.getToFileRange().getLineStart();
            final int endIndex = hunk.getToFileRange().getLineEnd();
            IntStream.rangeClosed(startIndex, endIndex)
                    .forEach(lineNumber -> textEditor.getMarkupModel()
                            .addLineHighlighter(lineNumber, HighlighterLayer.WARNING + 1, backgroundTextAttributes));
        });
        System.out.println("action=highlight_file status=finished");
    }

    private Optional<Diff> getDiff(final String absoluteFilePath, final String projectBasePath) {
        final String relativeFilePath = new File(projectBasePath)
                .toURI()
                .relativize(new File(absoluteFilePath).toURI())
                .toString();

        return diffs.stream().filter(diff -> diff.getToFileName().endsWith(relativeFilePath)).findFirst();
    }
}
