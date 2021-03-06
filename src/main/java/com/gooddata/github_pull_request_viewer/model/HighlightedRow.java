package com.gooddata.github_pull_request_viewer.model;

import com.intellij.openapi.editor.markup.RangeHighlighter;

public class HighlightedRow {

    private final int fileRowNumber;
    private final int diffRowNumber;
    private final String relativeFilePath;
    private final RangeHighlighter highlighter;

    public HighlightedRow(final int fileRowNumber, final int diffRowNumber,
                          final String relativeFilePath, final RangeHighlighter highlighter) {
        this.fileRowNumber = fileRowNumber;
        this.diffRowNumber = diffRowNumber;
        this.relativeFilePath = relativeFilePath;
        this.highlighter = highlighter;
    }

    public int getFileRowNumber() {
        return fileRowNumber;
    }

    public int getDiffRowNumber() {
        return diffRowNumber;
    }

    public RangeHighlighter getHighlighter() {
        return highlighter;
    }

    public String getRelativeFilePath() {
        return relativeFilePath;
    }
}
