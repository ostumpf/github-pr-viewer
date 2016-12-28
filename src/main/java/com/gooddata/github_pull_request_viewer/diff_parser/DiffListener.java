package com.gooddata.github_pull_request_viewer.diff_parser;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DiffListener extends GitHubDiffBaseListener {

    private final List<Diff.Builder> diffBuilders = new ArrayList<>();

    @Override
    public void exitDiff_header(final GitHubDiffParser.Diff_headerContext ctx) {
        super.exitDiff_header(ctx);

        diffBuilders.add(new Diff.Builder()
            .sourceCommit(ctx.index_line().compare_hashes().hash(0).getText())
            .targetCommit(ctx.index_line().compare_hashes().hash(1).getText())
            .sourceFile(ctx.source_file_line().path().getText())
            .targetFile(ctx.target_file_line().path().getText())
            .setOptions(ctx.declaration_line().OPTION().stream().map(TerminalNode::getText).collect(Collectors.toList()))
            .type(getFileType(ctx.file_mode_line()))
        );
    }

    @Override
    public void exitMissing_newline_line(final GitHubDiffParser.Missing_newline_lineContext ctx) {
        super.exitMissing_newline_line(ctx);

        final Diff.Builder currentBuilder = diffBuilders.get(diffBuilders.size() - 1);
        currentBuilder.missingLineEnd();
    }

    @Override
    public void exitHunk(final GitHubDiffParser.HunkContext ctx) {
        super.exitHunk(ctx);

        final Diff.Builder currentBuilder = diffBuilders.get(diffBuilders.size() - 1);
        final String sourceStartIndex = ctx.hunk_line().range(0).NUMBER(0).getText();
        final String sourceLength = ctx.hunk_line().range(0).NUMBER(1).getText();
        final String targetStartIndex = ctx.hunk_line().range(1).NUMBER(0).getText();
        final String targetLength = ctx.hunk_line().range(1).NUMBER(1).getText();
        final String headerLine = ctx.hunk_line().text().getText();

        currentBuilder.addHunk(new Hunk.Builder()
                .sourceStart(Integer.parseInt(sourceStartIndex))
                .sourceLength(Integer.parseInt(sourceLength))
                .targetStart(Integer.parseInt(targetStartIndex))
                .targetLength(Integer.parseInt(targetLength))
                .headerLine(headerLine)
                .setLines(ctx.line().stream().map(this::getLine).filter(Objects::nonNull).collect(Collectors.toList()))
                .build()
        );
    }

    public List<Diff> getDiffs() {
        return diffBuilders.stream().map(Diff.Builder::build).collect(Collectors.toList());
    }

    private Line getLine(final GitHubDiffParser.LineContext lineContext) {
        if (lineContext.added_line() != null) {
           return getLine(lineContext.added_line());
        }

        if (lineContext.removed_line() != null) {
            return getLine(lineContext.removed_line());
        }

        if (lineContext.neutral_line() != null) {
            return getLine(lineContext.neutral_line());
        }

        if (lineContext.missing_newline_line() != null) {
            return null;
        }

        throw new IllegalStateException("A line should be added, removed or neutral.");
    }

    private Line getLine(final GitHubDiffParser.Added_lineContext addedLineContext) {
        return new Line(addedLineContext.text().getText(), Line.Type.ADDED);
    }

    private Line getLine(final GitHubDiffParser.Removed_lineContext removedLineContext) {
        return new Line(removedLineContext.text().getText(), Line.Type.REMOVED);
    }

    private Line getLine(final GitHubDiffParser.Neutral_lineContext neutralLineContext) {
        final String whitespace = neutralLineContext.WS().getText().substring(1);
        final String text = neutralLineContext.text().getText();
        return new Line(whitespace + text, Line.Type.NEUTRAL);
    }

    private Diff.Type getFileType(final GitHubDiffParser.File_mode_lineContext file_mode_lineContext) {
        if (file_mode_lineContext == null) {
            return Diff.Type.MODIFIED;
        }
        if (file_mode_lineContext.deleted_file_line() != null) {
            return Diff.Type.DELETED;
        }

        if (file_mode_lineContext.new_file_line() != null) {
            return Diff.Type.NEW;
        }

        throw new IllegalStateException("A file should be added, removed or modified.");
    }
}
