package com.gooddata.github_pull_request_viewer.diff_parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Diff {

    private final String sourceFile, targetFile;
    private final String sourceCommit, targetCommit;
    private final List<String> options;
    private final List<Hunk> hunks;
    private final boolean missingLineEnd;
    private final Type type;

    public Diff(final Type type,
                final String targetFile,
                final String sourceCommit,
                final String targetCommit,
                final List<String> options,
                final String sourceFile,
                final boolean missingLineEnd,
                final List<Hunk> hunks) {
        this.type = type;
        this.sourceFile = sourceFile;
        this.targetFile = targetFile;
        this.sourceCommit = sourceCommit;
        this.targetCommit = targetCommit;
        this.options = options;
        this.hunks = hunks;
        this.missingLineEnd = missingLineEnd;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getTargetFile() {
        return targetFile;
    }

    public String getSourceCommit() {
        return sourceCommit;
    }

    public String getTargetCommit() {
        return targetCommit;
    }

    public String getRelativePath() {
        return targetFile.substring(2); // remove b/
    }

    public boolean isMissingLineEnd() {
        return missingLineEnd;
    }

    public List<String> getOptions() {
        return options;
    }

    public List<Hunk> getHunks() {
        return hunks;
    }

    @Override
    public String toString() {
        return "Diff{" +
                "sourceFile='" + sourceFile + '\'' +
                ", targetFile='" + targetFile + '\'' +
                ", sourceCommit='" + sourceCommit + '\'' +
                ", targetCommit='" + targetCommit + '\'' +
                ", options=" + options.size() +
                ", hunks=" + hunks.size() +
                ", missingLineEnd=" + missingLineEnd +
                ", type=" + type +
                '}';
    }

    public enum Type {
        NEW, DELETED, MODIFIED
    }

    public static class Builder {
        private String sourceFile, targetFile;
        private String sourceCommit, targetCommit;
        private List<String> options = new ArrayList<>();
        private List<Hunk> hunks = new ArrayList<>();
        private boolean missingLineEnd;
        private Type type;

        public Builder sourceFile(final String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder targetFile(final String targetFile) {
            this.targetFile = targetFile;
            return this;
        }

        public Builder sourceCommit(final String sourceCommit) {
            this.sourceCommit = sourceCommit;
            return this;
        }

        public Builder targetCommit(final String targetCommit) {
            this.targetCommit = targetCommit;
            return this;
        }

        public Builder setOptions(final List<String> options) {
            this.options = options;
            return this;
        }

        public Builder addHunk(final Hunk hunk) {
            this.hunks.add(hunk);
            return this;
        }

        public Builder missingLineEnd() {
            this.missingLineEnd = true;
            return this;
        }

        public Builder type(final Type type) {
            this.type = type;
            return this;
        }

        public Diff build() {
            return new Diff(
                    type, targetFile, sourceCommit, targetCommit, Collections.unmodifiableList(options), sourceFile,
                    missingLineEnd, Collections.unmodifiableList(hunks)
            );
        }
    }
}
