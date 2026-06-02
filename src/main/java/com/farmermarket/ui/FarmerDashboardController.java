package com.farmermarket.ui;

import com.farmermarket.model.Order;
import com.farmermarket.model.OrderStatus;
import com.farmermarket.model.Product;
import com.farmermarket.model.User;
import com.farmermarket.service.OrderService;
import com.farmermarket.service.ProductService;
import com.farmermarket.service.UserService;
import com.farmermarket.socket.NotificationServer;
import com.farmermarket.ui.util.AlertHelper;
import com.farmermarket.ui.util.TaskRunner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;


public class FarmerDashboardController {

    private final Stage          stage;
    private final User           currentUser;
    private final ProductService productService;
    private final OrderService   orderService;
    private final UserService    userService;
    private final NotificationServer notificationServer;

    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final ObservableList<Order>   orderList   = FXCollections.observableArrayList();

    private TableView<Product> productTable;
    private TextField     nameField, categoryField, priceField;
    private Spinner<Integer> quantitySpinner;
    private Button addBtn, updateBtn, deleteBtn;
    private ProgressIndicator productSpinner;

    private TableView<Order> orderTable;
    private Button markShippedBtn;
    private ProgressIndicator orderSpinner;

    public FarmerDashboardController(Stage stage, User currentUser,
                                     ProductService productService,
                                     OrderService orderService,
                                     UserService userService,
                                     NotificationServer notificationServer) {
        this.stage               = stage;
        this.currentUser         = currentUser;
        this.productService      = productService;
        this.orderService        = orderService;
        this.userService         = userService;
        this.notificationServer  = notificationServer;
    }



    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard-root");
        root.setTop(buildHeader());
        root.setCenter(buildTabPane());

        Scene scene = new Scene(root, 1060, 700);
        applyStylesheet(scene);
        loadProducts();
        loadOrders();
        return scene;
    }



    private HBox buildHeader() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setSpacing(14);
        bar.setPadding(new Insets(0, 24, 0, 24));
        bar.setPrefHeight(62);
        bar.setStyle(
                "-fx-background-color: linear-gradient(to right, #1b5e20, #2e7d32);" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 10, 0, 0, 3);"
        );

        Label icon = new Label("🌾");
        icon.setStyle("-fx-font-size: 26px;");

        Label title = new Label("Farmer Dashboard");
        title.setStyle(
                "-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stats chips
        HBox chips = new HBox(10);
        chips.setAlignment(Pos.CENTER);

        Label prodChip = chip("📦 Products");
        Label ordChip  = chip("📋 Orders");
        chips.getChildren().addAll(prodChip, ordChip);

        // User badge
        Label userBadge = new Label("👤 " + currentUser.getName());
        userBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); " +
                        "-fx-text-fill: white; -fx-font-size: 12px; " +
                        "-fx-padding: 5 12 5 12; -fx-background-radius: 20;"
        );

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.18); " +
                        "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        "-fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-cursor: hand; " +
                        "-fx-border-color: rgba(255,255,255,0.40); -fx-border-radius: 6; -fx-border-width: 1;"
        );
        logoutBtn.setOnAction(e -> navigateToLogin());

        bar.getChildren().addAll(icon, title, spacer, chips, userBadge, logoutBtn);
        return bar;
    }

    private Label chip(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color: rgba(255,255,255,0.12); " +
                        "-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px; " +
                        "-fx-padding: 4 12 4 12; -fx-background-radius: 20;"
        );
        return l;
    }



    private TabPane buildTabPane() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("tab-pane");
        tabs.getTabs().addAll(buildProductsTab(), buildOrdersTab());
        return tabs;
    }



    private Tab buildProductsTab() {
        Tab tab = new Tab("  📦  My Products  ");


        productTable = new TableView<>(productList);
        productTable.getStyleClass().add("table-view");
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPlaceholder(new Label("No products yet. Add your first product →"));

        productTable.getColumns().addAll(
                col("Name",       "name"),
                col("Category",   "category"),
                col("Quantity",   "quantity"),
                col("Price/Unit ₹", "pricePerUnit")
        );

        productTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> populateForm(sel));

        VBox tableCard = card(productTable);
        VBox.setVgrow(productTable, Priority.ALWAYS);

        // ── Form card ──
        Label formTitle = new Label("Add / Edit Product");
        formTitle.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;"
        );

        nameField     = styledField("Product name");
        categoryField = styledField("e.g. Vegetables, Grains");
        quantitySpinner = new Spinner<>(0, 100_000, 0);
        quantitySpinner.setEditable(true);
        quantitySpinner.setMaxWidth(Double.MAX_VALUE);
        priceField    = styledField("0.00");

        productSpinner = new ProgressIndicator();
        productSpinner.setPrefSize(22, 22);
        productSpinner.setVisible(false);
        productSpinner.setManaged(false);

        addBtn    = new Button("➕ Add");
        updateBtn = new Button("✏ Update");
        deleteBtn = new Button("🗑 Delete");
        addBtn.getStyleClass().add("primary-button");
        updateBtn.getStyleClass().add("secondary-button");
        deleteBtn.getStyleClass().add("danger-button");

        addBtn.setOnAction(e    -> handleAddProduct());
        updateBtn.setOnAction(e -> handleUpdateProduct());
        deleteBtn.setOnAction(e -> handleDeleteProduct());

        HBox btnRow = new HBox(8, addBtn, updateBtn, deleteBtn, productSpinner);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10);
        form.setPadding(new Insets(16));
        form.setStyle(
                "-fx-background-color: white; -fx-border-color: #eeeeee; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        form.add(formTitle,                    0, 0, 2, 1);
        form.add(fieldLabel("Name"),           0, 1); form.add(nameField,       1, 1);
        form.add(fieldLabel("Category"),       0, 2); form.add(categoryField,   1, 2);
        form.add(fieldLabel("Quantity"),       0, 3); form.add(quantitySpinner, 1, 3);
        form.add(fieldLabel("Price / Unit ₹"), 0, 4); form.add(priceField,      1, 4);
        form.add(btnRow,                       0, 5, 2, 1);

        ColumnConstraints c0 = new ColumnConstraints(110);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c0, c1);

        SplitPane split = new SplitPane(tableCard, form);
        split.setDividerPositions(0.62);
        VBox.setVgrow(split, Priority.ALWAYS);

        VBox content = new VBox(split);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #f5f5f5;");
        VBox.setVgrow(split, Priority.ALWAYS);

        tab.setContent(content);
        return tab;
    }



    private Tab buildOrdersTab() {
        Tab tab = new Tab("  📋  Incoming Orders  ");

        orderTable = new TableView<>(orderList);
        orderTable.getStyleClass().add("table-view");
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderTable.setPlaceholder(new Label("No incoming orders yet."));

        orderTable.getColumns().addAll(
                col("Order #",    "id"),
                col("Buyer",      "buyerName"),
                col("Product",    "productName"),
                col("Qty",        "quantity"),
                col("Total ₹",    "totalPrice"),
                col("Status",     "status")
        );

        orderSpinner = new ProgressIndicator();
        orderSpinner.setPrefSize(22, 22);
        orderSpinner.setVisible(false);
        orderSpinner.setManaged(false);

        markShippedBtn = new Button("🚚  Mark as Shipped");
        markShippedBtn.getStyleClass().add("primary-button");
        markShippedBtn.setOnAction(e -> handleMarkShipped());

        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> loadOrders());


        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.getChildren().addAll(
                legendDot("#ffb300", "Pending"),
                legendDot("#43a047", "Shipped"),
                legendDot("#1b5e20", "Delivered")
        );

        HBox toolbar = new HBox(10, markShippedBtn, refreshBtn, orderSpinner);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        topRow.getChildren().addAll(toolbar, sp, legend);

        VBox tableCard = card(orderTable);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        VBox content = new VBox(12, topRow, tableCard);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #f5f5f5;");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        tab.setContent(content);
        return tab;
    }

    private HBox legendDot(String color, String label) {
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
        dot.setFill(Color.web(color));
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        HBox row = new HBox(5, dot, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }



    private void handleAddProduct() {
        Product p = readForm();
        if (p == null) return;
        Task<Product> task = new Task<>() {
            @Override protected Product call() throws Exception {
                return productService.addProduct(p);
            }
        };
        bindSpinner(task, productSpinner);
        task.setOnSucceeded(ev -> { loadProducts(); clearForm(); AlertHelper.showInfo("Product added successfully."); });
        task.setOnFailed(ev -> AlertHelper.showError(task.getException().getMessage()));
        TaskRunner.run(task);
    }

    private void handleUpdateProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Select a product to update."); return; }
        Product p = readForm();
        if (p == null) return;
        p.setId(selected.getId());
        p.setFarmerId(selected.getFarmerId());
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                productService.updateProduct(p); return null;
            }
        };
        bindSpinner(task, productSpinner);
        task.setOnSucceeded(ev -> { loadProducts(); clearForm(); AlertHelper.showInfo("Product updated."); });
        task.setOnFailed(ev -> AlertHelper.showError(task.getException().getMessage()));
        TaskRunner.run(task);
    }

    private void handleDeleteProduct() {
        Product selected = productTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Select a product to delete."); return; }
        if (!AlertHelper.showConfirmation("Delete \"" + selected.getName() + "\"? This cannot be undone.")) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                productService.deleteProduct(selected.getId()); return null;
            }
        };
        bindSpinner(task, productSpinner);
        task.setOnSucceeded(ev -> { loadProducts(); clearForm(); });
        task.setOnFailed(ev -> AlertHelper.showError(task.getException().getMessage()));
        TaskRunner.run(task);
    }

    private void handleMarkShipped() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Select an order to mark as shipped."); return; }
        if (selected.getStatus() != OrderStatus.PENDING) {
            AlertHelper.showError("Only PENDING orders can be marked as shipped."); return;
        }
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                orderService.markShipped(selected.getId()); return null;
            }
        };
        bindSpinner(task, orderSpinner);
        task.setOnSucceeded(ev -> { loadOrders(); AlertHelper.showInfo("Order #" + selected.getId() + " marked as SHIPPED."); });
        task.setOnFailed(ev -> AlertHelper.showError(task.getException().getMessage()));
        TaskRunner.run(task);
    }



    private void loadProducts() {
        Task<List<Product>> task = new Task<>() {
            @Override protected List<Product> call() throws Exception {
                return productService.getProductsForFarmer(currentUser.getId());
            }
        };
        bindSpinner(task, productSpinner);
        task.setOnSucceeded(ev -> productList.setAll(task.getValue()));
        task.setOnFailed(ev -> AlertHelper.showError("Failed to load products."));
        TaskRunner.run(task);
    }

    private void loadOrders() {
        Task<List<Order>> task = new Task<>() {
            @Override protected List<Order> call() throws Exception {
                return orderService.getOrdersForFarmer(currentUser.getId());
            }
        };
        bindSpinner(task, orderSpinner);
        task.setOnSucceeded(ev -> orderList.setAll(task.getValue()));
        task.setOnFailed(ev -> AlertHelper.showError("Failed to load orders."));
        TaskRunner.run(task);
    }



    private Product readForm() {
        String name     = nameField.getText().trim();
        String category = categoryField.getText().trim();
        int    qty      = quantitySpinner.getValue();
        double price;
        try { price = Double.parseDouble(priceField.getText().trim()); }
        catch (NumberFormatException e) { AlertHelper.showError("Price must be a valid number."); return null; }
        return new Product(0, currentUser.getId(), name, category, qty, price);
    }

    private void populateForm(Product p) {
        if (p == null) return;
        nameField.setText(p.getName());
        categoryField.setText(p.getCategory());
        quantitySpinner.getValueFactory().setValue(p.getQuantity());
        priceField.setText(String.valueOf(p.getPricePerUnit()));
    }

    private void clearForm() {
        nameField.clear(); categoryField.clear();
        quantitySpinner.getValueFactory().setValue(0);
        priceField.clear();
        productTable.getSelectionModel().clearSelection();
    }



    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String title, String property) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    private <T> void bindSpinner(Task<T> task, ProgressIndicator spinner) {
        spinner.visibleProperty().bind(task.runningProperty());
        spinner.managedProperty().bind(task.runningProperty());
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-field-label");
        return l;
    }


    private VBox card(javafx.scene.Node node) {
        VBox box = new VBox(node);
        box.setStyle(
                "-fx-background-color: white; -fx-border-color: #eeeeee; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );
        VBox.setVgrow(node, Priority.ALWAYS);
        return box;
    }

    private void navigateToLogin() {
        LoginController ctrl =
                new LoginController(stage, userService, productService,
                        orderService, notificationServer);
        stage.setScene(ctrl.buildScene());
        stage.setTitle("Farmer Market — Login");
    }

    private void applyStylesheet(Scene scene) {
        var css = getClass().getResource("/com/farmermarket/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
    }
}
