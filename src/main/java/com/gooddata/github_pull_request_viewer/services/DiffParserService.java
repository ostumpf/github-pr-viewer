package com.gooddata.github_pull_request_viewer.services;

import com.gooddata.github_pull_request_viewer.diff_parser.Diff;
import com.gooddata.github_pull_request_viewer.diff_parser.DiffListener;
import com.gooddata.github_pull_request_viewer.diff_parser.GitHubDiffLexer;
import com.gooddata.github_pull_request_viewer.diff_parser.GitHubDiffParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DiffParserService {

    public static void main(String[] args) throws IOException {
        new DiffParserService().parse(new FileInputStream("/home/ondra/diff.txt"));
    }


    public List<Diff> parse(final InputStream inputStream) throws IOException {
        final ANTLRInputStream input = new ANTLRInputStream(inputStream);
        final GitHubDiffLexer lexer = new GitHubDiffLexer(input);

        final CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        final ParseTreeWalker walker = new ParseTreeWalker();
        final GitHubDiffParser parser = new GitHubDiffParser(tokenStream);

        final ParseTree parseTree = parser.root_statement();
        final DiffListener diffListener = new DiffListener();
        walker.walk(diffListener, parseTree);

        return diffListener.getDiffs();
    }
}
