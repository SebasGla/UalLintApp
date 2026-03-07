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
import java.net.URL;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.control.CheckBox;
import java.util.LinkedHashMap;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;

import antlr4gen.LinterLexer;
import antlr4gen.LinterParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.gui.Trees;

import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/*
 * The main JavaFX GUI application for the assembler linter.
 * Handles the text editor, diagnostic tables, rule toggles, and memory register management.
 */
public final class LinterGuiApp extends Application
{
    private final CodeArea editor;                        // Rich text editor component
    private final TableView<DiagnosticRow> table;         // Table showing lint errors/warnings
    private final Label status;                           // Bottom status bar text
    private final Label details;                          // Shows full error message of selected row
    private final TableView<RegisterEntry> registerTable; // LUT table for custom hardware registers
    private final ObservableList<RegisterEntry> registerRows;
    private final Path registerStorePath;                 // Where the register LUT is saved on disk
    private final Map<String, CheckBox> ruleCheckboxes;   // Toggles for turning rules on/off
    private Integer highlightedLine = null;               // Tracks currently highlighted error line
    private static final String LINE_HIGHLIGHT_STYLE = "-fx-background-color: rgba(255, 0, 0, 0.20);";

    private File currentFile; // The active assembly file loaded in the editor

    public LinterGuiApp()
    {
        this.editor = new CodeArea();
        this.table = new TableView<>();
        this.status = new Label("No file loaded.");
        this.details = new Label("Select a diagnostic to see details.");
        this.registerTable = new TableView<>();
        this.registerRows = FXCollections.observableArrayList();
        this.registerStorePath = RegisterTableStore.defaultPath();
        this.ruleCheckboxes = new LinkedHashMap<>();
    }

    /*
     * Main entry point for the JavaFX UI.
     * Sets up the layout, buttons, sidebars, and event listeners.
     */
    @Override
    public void start(Stage stage)
    {
        Button openBtn = new Button("Open File");
        Button runBtn = new Button("Run Lint");
        Button treeBtn = new Button("Parse Tree");

        openBtn.setOnAction(e -> openFile(stage));
        runBtn.setOnAction(e -> runLintAsync());
        treeBtn.setOnAction(e -> showParseTree());

        ToolBar toolBar = new ToolBar(openBtn, runBtn, treeBtn);

        // Add line numbers to the editor
        editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));

        configureTable();

        // Main layout split: Editor on the left, Errors on the right
        SplitPane split = new SplitPane();
        split.getItems().add(new VirtualizedScrollPane<>(editor));
        split.getItems().add(table);
        split.setDividerPositions(0.60);

        // Status bar area
        VBox bottom = new VBox();
        bottom.setSpacing(6);
        bottom.setPadding(new Insets(8));
        bottom.getChildren().add(details);
        bottom.getChildren().add(status);

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(split);
        root.setBottom(bottom);

        installDragAndDrop(root);

        // Sidebar for settings and registers
        Accordion sidebar = new Accordion();
        sidebar.getPanes().add(buildRegisterTablePane(stage));
        sidebar.getPanes().add(buildRulesPane());
        root.setLeft(sidebar);

        // Update details text when user clicks a different error in the table
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal == null)
            {
                details.setText("Select a diagnostic to see details.");
                return;
            }
            details.setText(newVal.getRuleId() + " [" + newVal.getSeverity() + "]: " + newVal.getMessage() + "  Offending: " + newVal.getOffendingText());
        });

        // Jump to the exact line in the editor when an error is selected
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, row) ->
        {
            if (row == null) return;
            jumpTo(row.getLine(), row.getColumn());
        });

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("ARM ASM Linter");
        stage.setScene(scene);

        // Load custom CSS for the editor (like error highlighting)
        URL css = LinterGuiApp.class.getResource("/editor.css");
        if (css != null)
        {
            scene.getStylesheets().add(css.toExternalForm());
        }
        else
        {
            System.err.println("editor.css not found on classpath");
        }

        stage.show();
    }

    /*
     * Sets up drag-and-drop so users can just drop .s/.asm files straight into the app.
     */
    private void installDragAndDrop(BorderPane root)
    {
        root.setOnDragOver(e ->
        {
            Dragboard db = e.getDragboard();
            if (db.hasFiles() == true)
            {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        root.setOnDragDropped(e ->
        {
            Dragboard db = e.getDragboard();
            boolean success = false;

            if (db.hasFiles() == true)
            {
                List<File> files = db.getFiles();

                if (files.isEmpty() == false)
                {
                    File f = files.get(0);
                    loadFileIntoEditor(f);
                    success = true;
                }
            }

            e.setDropCompleted(success);
            e.consume();
        });
    }

    /*
     * Reads the file from disk and pushes the text into the RichTextFX editor.
     */
    private void loadFileIntoEditor(File f)
    {
        String name = f.getName().toLowerCase();

        // Basic sanity check to ensure it's an assembly file
        if (name.endsWith(".s") == false && name.endsWith(".asm") == false)
        {
            status.setText("Unsupported file type: " + f.getName());
            return;
        }

        try
        {
            String text = Files.readString(f.toPath());
            currentFile = f;
            editor.replaceText(text);
            status.setText("Loaded: " + f.getName());
        }
        catch (Exception ex)
        {
            status.setText("Failed to read file: " + ex.getMessage());
        }
    }

    /*
     * Sets up the columns and data mapping for the diagnostics table.
     */
    private void configureTable()
    {
        TableColumn<DiagnosticRow, String> severityCol = new TableColumn<>("Severity");
        TableColumn<DiagnosticRow, String> ruleCol = new TableColumn<>("Rule");
        TableColumn<DiagnosticRow, Integer> lineCol = new TableColumn<>("Line");
        TableColumn<DiagnosticRow, Integer> colCol = new TableColumn<>("Col");
        TableColumn<DiagnosticRow, String> msgCol = new TableColumn<>("Message");

        // Map columns to the properties in the DiagnosticRow class
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

    /*
     * Shows a standard OS file picker to load an assembly file.
     */
    private void openFile(Stage stage)
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Assembly File");
        File f = chooser.showOpenDialog(stage);

        if (f == null) return;

        currentFile = f;

        try
        {
            String text = Files.readString(f.toPath());
            editor.replaceText(text);
            status.setText("Loaded: " + f.getName());
        }
        catch (Exception ex)
        {
            status.setText("Failed to read file: " + ex.getMessage());
        }
    }

    /*
     * Executes the linter in a background thread.
     * Running heavy parsing on the main thread would freeze the UI, so we use a JavaFX Task.
     */
    private void runLintAsync()
    {
        if (currentFile == null)
        {
            status.setText("Open a file first.");
            return;
        }

        status.setText("Running lint...");
        table.getItems().clear();

        // Collect which rules the user has actually checked on the sidebar
        List<String> enabledRules = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : ruleCheckboxes.entrySet())
        {
            if (entry.getValue().isSelected())
            {
                enabledRules.add(entry.getKey());
            }
        }

        // Background task definition
        Task<List<DiagnosticRow>> task = new Task<>()
        {
            @Override
            protected List<DiagnosticRow> call()
            {
                Map<Long, String> addrToName = buildAddrToNameLut();
                return LintFacade.run(
                        editor.getText(),
                        currentFile.getName(),
                        addrToName,
                        enabledRules
                );
            }
        };

        // Update UI when finished
        task.setOnSucceeded(e ->
        {
            table.getItems().setAll(task.getValue());
            status.setText("Done. Diagnostics: " + task.getValue().size());
        });

        // Handle crashes gracefully
        task.setOnFailed(e ->
        {
            Throwable ex = task.getException();
            status.setText("Lint failed: " + ex.getMessage());
        });

        Thread t = new Thread(task);
        t.setDaemon(true); // Don't block app shutdown
        t.start();
    }

    /*
     * Scrolls the editor to the specified line/column and highlights it.
     */
    private void jumpTo(int line, int column)
    {
        int paragraph = Math.max(line - 1, 0); // JavaFX paragraphs are 0-indexed
        int col = Math.max(column - 1, 0);

        clearPreviousHighlight();

        editor.moveTo(paragraph, col);
        editor.requestFocus();
        editor.showParagraphAtTop(paragraph);

        highlightLine(paragraph);
    }

    /*
     * Applies a CSS class to the specific line to visually flag an error.
     */
    private void highlightLine(int paragraph)
    {
        editor.setParagraphStyle(paragraph, List.of("lint-line-highlight"));
        highlightedLine = paragraph;
    }

    /*
     * Removes the error highlight from the previously selected line.
     */
    private void clearPreviousHighlight()
    {
        if (highlightedLine != null)
        {
            editor.setParagraphStyle(highlightedLine, List.of());
            highlightedLine = null;
        }
    }

    /*
     * Opens ANTLR's built-in GUI visualizer to show the raw parse tree.
     * Excellent for debugging grammar issues.
     */
    private void showParseTree()
    {
        try
        {
            LinterLexer lexer = new LinterLexer(CharStreams.fromString(editor.getText()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LinterParser parser = new LinterParser(tokens);

            ParseTree tree = parser.program();

            org.antlr.v4.gui.Trees.inspect(tree, parser);
        }
        catch (Exception ex)
        {
            status.setText("Parse tree failed: " + ex.getMessage());
        }
    }

    /*
     * Builds the UI panel that lets users define custom memory-mapped registers.
     * Crucial for linters to know which magic numbers correspond to hardware features.
     */
    private TitledPane buildRegisterTablePane(Stage stage)
    {
        loadRegisterRows();

        registerTable.setEditable(true);
        registerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<RegisterEntry, String> nameCol = new TableColumn<>("Register name");
        nameCol.setCellValueFactory(c -> c.getValue().registerNameProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        nameCol.setOnEditCommit(e ->
        {
            e.getRowValue().setRegisterName(e.getNewValue());
            saveRegisterRows();
        });
        nameCol.setPrefWidth(160);

        TableColumn<RegisterEntry, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(c -> c.getValue().addressProperty());
        addrCol.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        addrCol.setOnEditCommit(e ->
        {
            e.getRowValue().setAddress(normalizeAddress(e.getNewValue()));
            saveRegisterRows();
        });
        addrCol.setPrefWidth(140);

        registerTable.getColumns().add(nameCol);
        registerTable.getColumns().add(addrCol);

        registerTable.setItems(registerRows);

        Button importBtn = new Button("Import CSV");
        Button exportBtn = new Button("Export CSV");
        Button addBtn = new Button("Add");
        Button removeBtn = new Button("Remove");

        importBtn.setOnAction(e -> importRegisterCsv(stage));
        exportBtn.setOnAction(e -> exportRegisterCsv(stage));

        addBtn.setOnAction(e ->
        {
            registerRows.add(new RegisterEntry("REG_NAME", "0x00000000"));
            saveRegisterRows();
        });

        removeBtn.setOnAction(e ->
        {
            RegisterEntry sel = registerTable.getSelectionModel().getSelectedItem();
            if (sel != null)
            {
                registerRows.remove(sel);
                saveRegisterRows();
            }
        });

        ToolBar bar = new ToolBar(importBtn, exportBtn, addBtn, removeBtn);

        VBox box = new VBox();
        box.getChildren().add(bar);
        box.getChildren().add(registerTable);

        TitledPane pane = new TitledPane("Register table", box);
        pane.setCollapsible(true);

        return pane;
    }

    /*
     * Loads saved register mappings from a local file.
     */
    private void loadRegisterRows()
    {
        registerRows.clear();
        registerRows.addAll(RegisterTableStore.load(registerStorePath));
    }

    /*
     * Saves the current register mappings to disk so they persist between sessions.
     */
    private void saveRegisterRows()
    {
        RegisterTableStore.save(registerStorePath, new ArrayList<>(registerRows));
        status.setText("Register table saved: " + registerStorePath.getFileName());
    }

    private void importRegisterCsv(Stage stage)
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import register LUT (CSV)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = chooser.showOpenDialog(stage);

        if (f == null) return;

        List<RegisterEntry> loaded = RegisterTableStore.load(f.toPath());
        registerRows.setAll(loaded);
        saveRegisterRows();
    }

    private void exportRegisterCsv(Stage stage)
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export register LUT (CSV)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = chooser.showSaveDialog(stage);

        if (f == null) return;

        RegisterTableStore.save(f.toPath(), new ArrayList<>(registerRows));
        status.setText("Exported: " + f.getName());
    }

    /*
     * Ensures addresses entered by the user are properly formatted as hex.
     * Converts "1A" to "0x1A" automatically.
     */
    private String normalizeAddress(String s)
    {
        if (s == null) return "0x00000000";

        String t = s.trim();
        if (t.isEmpty() == true) return "0x00000000";

        if (t.startsWith("0x") == false && t.startsWith("0X") == false)
        {
            if (t.matches("[0-9a-fA-F]+") == true)
            {
                return "0x" + t.toUpperCase();
            }
        }

        return t;
    }

    /*
     * Converts the UI table of registers into a highly efficient Map (LUT).
     * The linter uses this during execution to quickly look up memory addresses.
     */
    private Map<Long, String> buildAddrToNameLut()
    {
        Map<Long, String> m = new HashMap<>();

        for (RegisterEntry r : registerRows)
        {
            if (r == null) continue;

            String name = r.getRegisterName();
            String addrText = r.getAddress();

            Long addr = tryParseHexAddress(addrText);

            if (addr == null || name == null) continue;

            String trimmedName = name.trim();
            if (trimmedName.isEmpty() == true) continue;

            // Don't overwrite if multiple names point to the same address; keep the first one
            if (m.containsKey(addr) == false)
            {
                m.put(addr, trimmedName);
            }
        }

        return m;
    }

    /*
     * Safely attempts to parse a hex string into a numeric Long.
     * Returns null if the user typed garbage instead of crashing the app.
     */
    private Long tryParseHexAddress(String s)
    {
        if (s == null) return null;

        String t = s.trim();
        if (t.isEmpty() == true) return null;

        if (t.startsWith("0x") == true || t.startsWith("0X") == true)
        {
            t = t.substring(2);
        }

        if (t.isEmpty() == true) return null;

        try
        {
            return Long.parseUnsignedLong(t, 16);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    public static void main(String[] args)
    {
        launch(args);
    }

    /*
     * Builds the sidebar panel containing toggles for every single linting rule.
     * Allows the user to silence rules they don't care about.
     */
    private TitledPane buildRulesPane()
    {
        VBox box = new VBox();
        box.setSpacing(8);
        box.setPadding(new Insets(8));

        // Hardcoded list of all available rules
        String[] rules = {
                "BlankEof", "ItBlock", "CondFlags", "ImmediateHash",
                "AbsoluteAddress", "ShortInstructionForm", "ShiftPow2", "ThumbFunc",
                "NoCondReturns", "ParamsAAPCS", "RegisterFromManualEqu",
                "RoutineReturn", "LogicalOp", "RecursiveRoutine"
        };

        for (String rule : rules)
        {
            CheckBox cb = new CheckBox(rule);
            cb.setSelected(true); // Default to having all rules turned on
            ruleCheckboxes.put(rule, cb);
            box.getChildren().add(cb);
        }

        TitledPane pane = new TitledPane("Lint Rules", box);
        pane.setCollapsible(true);
        return pane;
    }
}