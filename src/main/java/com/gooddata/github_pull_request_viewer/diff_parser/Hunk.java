package com.gooddata.github_pull_request_viewer.diff_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hunk {

    private final int sourceStart, targetStart, sourceLength, targetLength;
    private final String headerLine;
    private final List<Line> lines;

    public Hunk(final int sourceStart,
                final int targetStart,
                final int sourceLength,
                final int targetLength,
                final String headerLine,
                final List<Line> lines) {
        this.sourceStart = sourceStart;
        this.targetStart = targetStart;
        this.sourceLength = sourceLength;
        this.targetLength = targetLength;
        this.headerLine = headerLine;
        this.lines = lines;
    }

    public int getSourceStart() {
        return sourceStart;
    }

    public int getTargetStart() {
        return targetStart;
    }

    public int getSourceLength() {
        return sourceLength;
    }

    public int getTargetLength() {
        return targetLength;
    }

    public String getHeaderLine() {
        return headerLine;
    }

    public List<Line> getLines() {
        return lines;
    }

    @Override
    public String toString() {
        return "Hunk{" +
                "sourceStart=" + sourceStart +
                ", targetStart=" + targetStart +
                ", sourceLength=" + sourceLength +
                ", targetLength=" + targetLength +
                ", lines=" + lines.size() +
                '}';
    }

    public static class Builder {
        private int sourceStart, targetStart, sourceLength, targetLength;
        private String headerLine;
        private List<Line> lines = new ArrayList<>();

        public Builder sourceStart(final int sourceStart) {
            this.sourceStart = sourceStart;
            return this;
        }

        public Builder targetStart(final int targetStart) {
            this.targetStart = targetStart;
            return this;
        }

        public Builder sourceLength(final int sourceLength) {
            this.sourceLength = sourceLength;
            return this;
        }

        public Builder targetLength(final int targetLength) {
            this.targetLength = targetLength;
            return this;
        }

        public Builder headerLine(final String headerLine) {
            this.headerLine = headerLine;
            return this;
        }

        public Builder setLines(final List<Line> lines) {
            this.lines = lines;
            return this;
        }

        public Hunk build() {
            return new Hunk(sourceStart, targetStart, sourceLength, targetLength, headerLine, Collections.unmodifiableList(lines));
        }
    }
}
