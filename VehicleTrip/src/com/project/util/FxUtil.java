package com.project.util;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Optional;

/**
 * Utility class that provides factory methods for common UI components,
 * replacing Swing JOptionPane, styled buttons, and TableView helpers.
 */
public class FxUtil {

    // ── CSS path ─────────────────────────────────────────────────────────────
    public static final String CSS = FxUtil.class
            .getResource("/com/project/style/style.css").toExternalForm();

    // ── Alerts (replaces JOptionPane) ────────────────────────────────────────

    public static void showInfo(Node owner, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        
        if (owner != null && owner.getScene() != null) {
            a.initOwner(owner.getScene().getWindow()); // ✅ CENTER FIX
        }
        
        a.showAndWait();
    }

    public static void showError(Node owner, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        
        if (owner != null && owner.getScene() != null) {
            a.initOwner(owner.getScene().getWindow()); // ✅ CENTER FIX
        }

        
        a.showAndWait();
    }

    public static void showWarning(Node owner, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        
        if (owner != null && owner.getScene() != null) {
            a.initOwner(owner.getScene().getWindow()); // ✅ CENTER FIX
        }
        
        a.showAndWait();
    }

    /** Returns true if user chose YES. */
    public static boolean confirm(Node owner, String msg, String title) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(null);
        
        if (owner != null && owner.getScene() != null) {
            a.initOwner(owner.getScene().getWindow()); // ✅ CENTER FIX
        }
        
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    /** Custom option dialog – returns index of chosen option (0-based), or -1. */
    public static int showOptions(Node owner, String msg, String title, String... options) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getButtonTypes().setAll();
        for (String opt : options) a.getButtonTypes().add(new ButtonType(opt));
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty()) return -1;
        for (int i = 0; i < options.length; i++) {
            if (r.get().getText().equals(options[i])) return i;
        }
        return -1;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    public static Button btnPrimary(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-primary");
        return b;
    }

    public static Button btnDanger(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-danger");
        return b;
    }

    public static Button btnSuccess(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-success");
        return b;
    }

    public static Button btnWarning(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-warning");
        return b;
    }

    public static Button btnOutlinePrimary(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-outline-primary");
        return b;
    }

    public static Button btnOutlineDanger(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-outline-danger");
        return b;
    }

    public static Button btnDark(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", "btn-dark");
        return b;
    }

    // ── Form helpers ──────────────────────────────────────────────────────────

    public static TextField styledField() {
        TextField f = new TextField();
        f.getStyleClass().add("form-field");
        f.setPrefWidth(260);
        f.setPrefHeight(32);
        return f;
    }

    public static PasswordField styledPasswordField() {
        PasswordField f = new PasswordField();
        f.getStyleClass().add("password-field");
        f.setPrefWidth(260);
        f.setPrefHeight(32);
        return f;
    }

    public static TextField readonlyField() {
        TextField f = new TextField();
        f.getStyleClass().add("form-field-readonly");
        f.setEditable(false);
        f.setPrefWidth(260);
        f.setPrefHeight(32);
        return f;
    }

    public static Label formLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    public static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-label");
        return l;
    }

    public static <T> ComboBox<T> styledCombo(ObservableList<T> items) {
        ComboBox<T> c = new ComboBox<>(items);
        c.getStyleClass().add("combo-field");
        c.setPrefWidth(260);
        c.setPrefHeight(32);
        return c;
    }

    // ── Status label ──────────────────────────────────────────────────────────

    public static Label statusLabel() {
        Label l = new Label(" ");
        l.getStyleClass().add("form-label");
        return l;
    }

    public static void setSuccess(Label l, String msg) {
        l.getStyleClass().removeAll("status-error", "status-success");
        l.getStyleClass().add("status-success");
        l.setText(msg);
    }

    public static void setError(Label l, String msg) {
        l.getStyleClass().removeAll("status-error", "status-success");
        l.getStyleClass().add("status-error");
        l.setText(msg);
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    public static Region spacer(double h) {
        Region r = new Region();
        r.setPrefHeight(h);
        r.setMinHeight(h);
        r.setMaxHeight(h);
        return r;
    }

    public static Region hspacer(double w) {
        Region r = new Region();
        r.setPrefWidth(w);
        r.setMinWidth(w);
        r.setMaxWidth(w);
        return r;
    }

    public static Region hgrow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Region vgrow() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    // ── TableView helpers ─────────────────────────────────────────────────────

    /**
     * Creates a TableView<Object[]> with columns built from the given header strings.
     * Each column retrieves data[colIndex] via cell value factory.
     */
    @SuppressWarnings("unchecked")
    public static TableView<Object[]> buildTable(String... headers) {
        TableView<Object[]> table = new TableView<>();
        table.getStyleClass().add("styled-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        for (int i = 0; i < headers.length; i++) {
            final int idx = i;
            TableColumn<Object[], Object> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue()[idx]));
            col.setSortable(false);
            table.getColumns().add(col);
        }
        return table;
    }

    public static ObservableList<Object[]> tableData(TableView<Object[]> table) {
        ObservableList<Object[]> data = FXCollections.observableArrayList();
        table.setItems(data);
        return data;
    }

    /** Wraps a TableView in a styled ScrollPane. */
    public static ScrollPane tableScroll(TableView<?> table) {
        ScrollPane sp = new ScrollPane(table);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    /**
     * Applies a CSS cell-renderer for the status column at index {@code col}.
     * Colouring mirrors the Swing StatusRenderer and custom renderers.
     */
    public static void applyStatusRenderer(TableView<Object[]> table, int col) {
        TableColumn<Object[], Object> c = (TableColumn<Object[], Object>) table.getColumns().get(col);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-pending","status-approved","status-completed",
                        "status-cancelled","status-active","status-inactive",
                        "status-available","status-success","status-error");
                if (empty || item == null) { setText(null); return; }
                String s = item.toString();
                setText(s);
                switch (s.toLowerCase()) {
                    case "pending"       -> getStyleClass().add("status-pending");
                    case "approved"      -> getStyleClass().add("status-approved");
                    case "completed","available" -> getStyleClass().add("status-completed");
                    case "cancelled","not available","inactive" -> getStyleClass().add("status-cancelled");
                    case "active"        -> getStyleClass().add("status-active");
                    case "logged in"     -> getStyleClass().add("status-completed");
                    case "logged out"    -> getStyleClass().add("status-cancelled");
                }
            }
        });
    }

    // ── Form row helper (GridPane) ────────────────────────────────────────────

    public static void addFormRow(GridPane grid, String labelText, Node field, int row) {
        Label lbl = formLabel(labelText);
        grid.add(lbl, 0, row);
        grid.add(field, 1, row);
        GridPane.setMargin(lbl,   new Insets(6, 10, 6, 10));
        GridPane.setMargin(field, new Insets(6, 10, 6, 10));
    }

    public static void addInfoRow(GridPane grid, String labelText, Label value, int row) {
        Label lbl = formLabel(labelText);
        grid.add(lbl,   0, row);
        grid.add(value, 1, row);
        GridPane.setMargin(lbl,   new Insets(6, 10, 6, 10));
        GridPane.setMargin(value, new Insets(6, 10, 6, 10));
    }

    /** Creates a standard GridPane for forms. */
    public static GridPane formGrid() {
        GridPane g = new GridPane();
        g.setBackground(Background.fill(Color.WHITE));
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(120);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }
}