import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.wickedsource.diffparser.api.UnifiedDiffParser;
import org.wickedsource.diffparser.api.model.Diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class PRViewer extends AnAction {
    private String token;
    private String userName;

    public PRViewer(final String userName, final String token) {
        super();
        this.token = token;
        this.userName = userName;
    }

    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("run with two arguments <github_username> <github_token>");
            System.exit(1);
        }

        new PRViewer(args[0], args[1]).actionPerformed(null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet("https://api.github.com/repos/gooddata/gdc-datawarehouse/pulls/398");
            request.addHeader("Accept", "application/vnd.github.v3.diff");
            request.addHeader("Authorization", "token " + token);
            request.addHeader("User-Agent", userName);
            HttpResponse response =  client.execute(request);

            final String content = read(response.getEntity().getContent());
            System.out.println("content: " + content);

            final List<Diff>  diffs = new UnifiedDiffParser().parse(content.getBytes("UTF-8"));
            diffs.forEach(diff -> {
                System.out.println(diff.getFromFileName());
                System.out.println(diff.getToFileName());
                diff.getHeaderLines().forEach(System.out::println);
                diff.getHunks().forEach(hunk -> {
                    System.out.println(hunk.getFromFileRange().getLineStart());
                    System.out.println(hunk.getToFileRange().getLineStart());
                    hunk.getLines().forEach(line -> {
                        System.out.println(line.getLineType());
                    });
                });
            });

        } catch (Exception e1) {
            e1.printStackTrace();
        }

        /*
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(e.getProject());
        final MessageBus messageBus = e.getProject().getMessageBus();

        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {

            }

            @Override
            public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile virtualFile) {

            }

            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent fileEditorManagerEvent) {

            }
        });

        final Editor textEditor = fileEditorManager.getSelectedTextEditor();
        System.out.println(textEditor.getDocument().getLineCount());

        textEditor.getContentComponent().setBackground(JBColor.BLACK);
        textEditor.getComponent().setBackground(JBColor.BLACK);

        final TextAttributes backgroundTextAttributes = new TextAttributes();
        backgroundTextAttributes.setBackgroundColor(JBColor.orange);


        textEditor.getMarkupModel().addRangeHighlighter(1, 200, HighlighterLayer.WARNING + 1, backgroundTextAttributes, HighlighterTargetArea.LINES_IN_RANGE);
        System.out.println("hello world!");
         */
    }

    public static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }
}
