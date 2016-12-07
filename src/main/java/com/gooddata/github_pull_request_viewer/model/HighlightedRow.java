package com.gooddata.github_pull_request_viewer.model;

import com.intellij.openapi.editor.markup.RangeHighlighter;

public class HighlightedRow {

    private final int fileRowNumber;
    private final int diffRownNumber;
    private final String commit;
    private final RangeHighlighter highlighter;

    public HighlightedRow(final int fileRowNumber, final int diffRownNumber, final String commit,
                          final RangeHighlighter highlighter) {
        this.fileRowNumber = fileRowNumber;
        this.diffRownNumber = diffRownNumber;
        this.commit = commit;
        this.highlighter = highlighter;
    }

    public int getFileRowNumber() {
        return fileRowNumber;
    }

    public int getDiffRownNumber() {
        return diffRownNumber;
    }

    public String getCommit() {
        return commit;
    }

    public RangeHighlighter getHighlighter() {
        return highlighter;
    }
}
