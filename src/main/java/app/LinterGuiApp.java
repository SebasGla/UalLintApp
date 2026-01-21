package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public final class LinterGuiApp extends Application
{
    private final TextArea editor;
    private final TableView<DiagnosticRow> table;
    private final Label status;
    private final Label details;

    private File currentFile;

    public LinterGuiApp()
    {
        this.editor = new TextArea();
        this.table = new TableView<>();
        this.status = new Label("No file loaded.");
        this.details = new Label("Select a diagnostic to see details.");
    }

    @Override
    public void start(Stage stage)
    {
        Button openBtn = new Button("Open File");
        Button runBtn = new Button("Run Lint");

        openBtn.setOnAction(e ->
        {
            openFile(stage);
        });

        runBtn.setOnAction(e ->
        {
            runLintAsync();
        });

        ToolBar toolBar = new ToolBar(openBtn, runBtn);

        configureTable();

        SplitPane split = new SplitPane();
        split.getItems().add(editor);
        split.getItems().add(table);
        split.setDividerPositions(0.60);

        VBox bottom = new VBox();
        bottom.setSpacing(6);
        bottom.setPadding(new Insets(8));
        bottom.getChildren().add(details);
        bottom.getChildren().add(status);

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(split);
        root.setBottom(bottom);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal == null)
            {
                details.setText("Select a diagnostic to see details.");
                return;
            }
            details.setText(newVal.getRuleId() + " [" + newVal.getSeverity() + "]: " + newVal.getMessage() + "  Offending: " + newVal.getOffendingText());
        });

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("ARM ASM Linter");
        stage.setScene(scene);
        stage.show();
    }

    private void configureTable()
    {
        TableColumn<DiagnosticRow, String> severityCol = new TableColumn<>("Severity");
        TableColumn<DiagnosticRow, String> ruleCol = new TableColumn<>("Rule");
        TableColumn<DiagnosticRow, Integer> lineCol = new TableColumn<>("Line");
        TableColumn<DiagnosticRow, Integer> colCol = new TableColumn<>("Col");
        TableColumn<DiagnosticRow, String> msgCol = new TableColumn<>("Message");

        severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        ruleCol.setCellValueFactory(new PropertyValueFactory<>("ruleId"));
        lineCol.setCellValueFactory(new PropertyValueFactory<>("line"));
        colCol.setCellValueFactory(new PropertyValueFactory<>("column"));
        msgCol.setCellValueFactory(new PropertyValueFactory<>("message"));

        severityCol.setPrefWidth(90);
        ruleCol.setPrefWidth(140);
        lineCol.setPrefWidth(70);
        colCol.setPrefWidth(70);
        msgCol.setPrefWidth(520);

        table.getColumns().add(severityCol);
        table.getColumns().add(ruleCol);
        table.getColumns().add(lineCol);
        table.getColumns().add(colCol);
        table.getColumns().add(msgCol);

        table.setItems(FXCollections.observableArrayList());
    }

    private void openFile(Stage stage)
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Assembly File");
        File f = chooser.showOpenDialog(stage);

        if (f == null)
        {
            return;
        }

        currentFile = f;

        try
        {
            String text = Files.readString(f.toPath());
            editor.setText(text);
            status.setText("Loaded: " + f.getName());
        }
        catch (Exception ex)
        {
            status.setText("Failed to read file: " + ex.getMessage());
        }
    }

    private void runLintAsync()
    {
        if (currentFile == null)
        {
            status.setText("Open a file first.");
            return;
        }

        status.setText("Running lint...");
        table.getItems().clear();

        Task<List<DiagnosticRow>> task = new Task<>()
        {
            @Override
            protected List<DiagnosticRow> call()
            {
                return LintFacade.run(
                        editor.getText(),
                        currentFile.getName()
                );
            }
        };

        task.setOnSucceeded(e ->
        {
            table.getItems().setAll(task.getValue());
            status.setText("Done. Diagnostics: " + task.getValue().size());
        });

        task.setOnFailed(e ->
        {
            Throwable ex = task.getException();
            status.setText("Lint failed: " + ex.getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }


    public static void main(String[] args)
    {
        launch(args);
    }
}
