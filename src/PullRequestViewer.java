import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.wickedsource.diffparser.api.UnifiedDiffParser;
import org.wickedsource.diffparser.api.model.Diff;

import java.util.List;

public class PullRequestViewer extends AnAction {

    public static void main(String[] args) {
        new PullRequestViewer().actionPerformed(null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet("https://api.github.com/repos/gooddata/gdc-datawarehouse/pulls/398");
            request.addHeader("Accept", "application/vnd.github.v3.diff");
            request.addHeader("Authorization", "token b1ca0a3e53fcebeac73139fd9b22348f15e3f138");
            request.addHeader("User-Agent", "ostumpf");
            HttpResponse response =  client.execute(request);

            final List<Diff>  diffs = new UnifiedDiffParser().parse(response.getEntity().getContent());
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
}
