package com.farmermarket.ui;

import com.farmermarket.model.Order;
import com.farmermarket.model.OrderStatus;
import com.farmermarket.model.Product;
import com.farmermarket.model.User;
import com.farmermarket.service.OrderService;
import com.farmermarket.service.ProductService;
import com.farmermarket.service.UserService;
import com.farmermarket.socket.NotificationClient;
import com.farmermarket.socket.NotificationServer;
import com.farmermarket.ui.util.AlertHelper;
import com.farmermarket.ui.util.CatalogFilter;
import com.farmermarket.ui.util.TaskRunner;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class BuyerDashboardController {

    private final Stage          stage;
    private final User           currentUser;
    private final ProductService productService;
    private final OrderService   orderService;
    private final UserService    userService;
    private final NotificationServer notificationServer;


    private NotificationClient notificationClient;

    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;

    private TableView<Product> catalogTable;
    private TextField     searchField;
    private ComboBox<String> categoryFilter;
    private ProgressIndicator catalogSpinner;

    private Spinner<Integer> qtySpinner;
    private Button placeOrderBtn;
    private Label  orderErrorLabel;
    private ProgressIndicator orderSpinner;

    private VBox deliveryContainer;
    private final Set<Integer> activeDeliveries = new HashSet<>();
    private ScheduledFuture<?> priceUpdaterFuture;

    public BuyerDashboardController(Stage stage, User currentUser,
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
        root.setStyle("-fx-background-color: #f5f5f5;");

        root.setTop(buildTopSection());

        HBox mainContent = new HBox(14);
        mainContent.setPadding(new Insets(14, 14, 0, 14));
        mainContent.setStyle("-fx-background-color: #f5f5f5;");

        VBox catalogSection = buildCatalogSection();
        HBox.setHgrow(catalogSection, Priority.ALWAYS);

        VBox orderPanel = buildOrderPanel();

        mainContent.getChildren().addAll(catalogSection, orderPanel);
        root.setCenter(mainContent);

        root.setBottom(buildDeliverySection());

        Scene scene = new Scene(root, 1120, 740);
        applyStylesheet(scene);

        loadCatalog();
        startPriceUpdater();
        loadActiveDeliveries();
        connectNotificationClient();

        return scene;
    }



    private VBox buildTopSection() {
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(14);
        header.setPadding(new Insets(0, 24, 0, 24));
        header.setPrefHeight(62);
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #1b5e20, #2e7d32);" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 10, 0, 0, 3);"
        );

        Label icon = new Label("🛒");
        icon.setStyle("-fx-font-size: 26px;");

        Label title = new Label("Buyer Dashboard");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        Label liveBadge = new Label("🔴 LIVE Prices");
        liveBadge.setStyle(
                "-fx-background-color: rgba(255,179,0,0.25); " +
                        "-fx-text-fill: #ffb300; -fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-padding: 4 10 4 10; -fx-background-radius: 20; " +
                        "-fx-border-color: rgba(255,179,0,0.50); -fx-border-radius: 20; -fx-border-width: 1;"
        );

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
        logoutBtn.setOnAction(e -> {
            if (notificationClient != null) notificationClient.disconnect();
            if (priceUpdaterFuture != null) priceUpdaterFuture.cancel(false);
            navigateToLogin();
        });

        header.getChildren().addAll(icon, title, spacer, liveBadge, userBadge, logoutBtn);


        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(10, 20, 10, 20));
        searchBar.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: transparent transparent #eeeeee transparent; " +
                        "-fx-border-width: 0 0 1 0;"
        );

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 15px;");

        searchField = new TextField();
        searchField.setPromptText("Search products by name or category...");
        searchField.setPrefWidth(300);
        searchField.setStyle(
                "-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; " +
                        "-fx-border-radius: 20; -fx-background-radius: 20; " +
                        "-fx-padding: 7 14 7 14; -fx-font-size: 13px;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);

        categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("All Categories");
        categoryFilter.setPrefWidth(180);

        catalogSpinner = new ProgressIndicator();
        catalogSpinner.setPrefSize(20, 20);
        catalogSpinner.setVisible(false);
        catalogSpinner.setManaged(false);

        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(e -> loadCatalog());

        searchBar.getChildren().addAll(searchIcon, searchField, categoryFilter,
                refreshBtn, catalogSpinner);

        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, val) -> applyFilters());

        return new VBox(header, searchBar);
    }



    private VBox buildCatalogSection() {
        filteredProducts = new FilteredList<>(allProducts, p -> true);

        catalogTable = new TableView<>(filteredProducts);
        catalogTable.getStyleClass().add("table-view");
        catalogTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        catalogTable.setPlaceholder(new Label("No products available."));

        TableColumn<Product, String>  nameCol  = col("Product Name", "name");
        TableColumn<Product, String>  catCol   = col("Category",     "category");
        TableColumn<Product, Integer> qtyCol   = col("In Stock",     "quantity");

        // Price column
        TableColumn<Product, Double> priceCol = new TableColumn<>("Listed Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("pricePerUnit"));
        priceCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("₹%.2f", val));
            }
        });


        TableColumn<Product, Double> fairCol = new TableColumn<>("⚖ Fair Price");
        fairCol.setCellValueFactory(new PropertyValueFactory<>("fairPrice"));
        fairCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); }
                else {
                    setText(String.format("₹%.2f", val));
                    setStyle(
                            "-fx-text-fill: #f57f17; -fx-font-weight: bold; " +
                                    "-fx-background-color: #fff8e1;"
                    );
                }
            }
        });

        catalogTable.getColumns().addAll(nameCol, catCol, qtyCol, priceCol, fairCol);
        VBox.setVgrow(catalogTable, Priority.ALWAYS);


        Label sectionLbl = new Label("🌿  Available Products");
        sectionLbl.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;"
        );

        VBox wrapper = new VBox(8, sectionLbl, catalogTable);
        wrapper.setStyle(
                "-fx-background-color: white; -fx-border-color: #eeeeee; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; " +
                        "-fx-padding: 14; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );
        VBox.setVgrow(catalogTable, Priority.ALWAYS);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }



    private VBox buildOrderPanel() {
        VBox panel = new VBox(14);
        panel.setPrefWidth(230);
        panel.setMaxWidth(230);
        panel.setPadding(new Insets(18));
        panel.setStyle(
                "-fx-background-color: white; -fx-border-color: #eeeeee; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );

        Label title = new Label("🛒  Place Order");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;");

        Separator sep = new Separator();

        Label hint = new Label("Select a product from the table, then set your quantity.");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #9e9e9e;");

        Label qtyLbl = new Label("Quantity");
        qtyLbl.getStyleClass().add("form-field-label");

        qtySpinner = new Spinner<>(1, 100_000, 1);
        qtySpinner.setEditable(true);
        qtySpinner.setMaxWidth(Double.MAX_VALUE);

        orderErrorLabel = new Label();
        orderErrorLabel.getStyleClass().add("error-label");
        orderErrorLabel.setVisible(false);
        orderErrorLabel.setManaged(false);
        orderErrorLabel.setWrapText(true);

        placeOrderBtn = new Button("🛒  Place Order");
        placeOrderBtn.getStyleClass().add("primary-button");
        placeOrderBtn.setMaxWidth(Double.MAX_VALUE);
        placeOrderBtn.setPrefHeight(42);
        placeOrderBtn.setOnAction(e -> handlePlaceOrder());

        orderSpinner = new ProgressIndicator();
        orderSpinner.setPrefSize(22, 22);
        orderSpinner.setVisible(false);
        orderSpinner.setManaged(false);


        VBox priceInfo = new VBox(6);
        priceInfo.setStyle(
                "-fx-background-color: #e8f5e9; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-padding: 10;"
        );
        Label priceInfoTitle = new Label("Selected Product");
        priceInfoTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575; -fx-font-weight: bold;");
        Label priceInfoVal = new Label("—");
        priceInfoVal.setStyle("-fx-font-size: 13px; -fx-text-fill: #1b5e20; -fx-font-weight: bold;");
        priceInfoVal.setWrapText(true);
        priceInfo.getChildren().addAll(priceInfoTitle, priceInfoVal);


        panel.getChildren().addAll(
                title, sep, hint,
                priceInfo,
                qtyLbl, qtySpinner,
                orderErrorLabel,
                placeOrderBtn, orderSpinner
        );


        javafx.application.Platform.runLater(() -> {
            if (catalogTable != null) {
                catalogTable.getSelectionModel().selectedItemProperty().addListener(
                        (obs, old, sel) -> {
                            if (sel != null) {
                                priceInfoVal.setText(
                                        sel.getName() + "\n" +
                                                "Listed: ₹" + String.format("%.2f", sel.getPricePerUnit()) + "\n" +
                                                "Fair:   ₹" + String.format("%.2f", sel.getFairPrice())
                                );
                            } else {
                                priceInfoVal.setText("—");
                            }
                        }
                );
            }
        });

        return panel;
    }



    private TitledPane buildDeliverySection() {
        deliveryContainer = new VBox(10);
        deliveryContainer.setPadding(new Insets(10));

        Label empty = new Label("No active deliveries.");
        empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px;");
        deliveryContainer.getChildren().add(empty);

        ScrollPane scroll = new ScrollPane(deliveryContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(170);
        scroll.setStyle("-fx-background-color: white; -fx-border-color: transparent;");

        TitledPane pane = new TitledPane("  📦  My Orders & Delivery Tracking", scroll);
        pane.setExpanded(false);
        pane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #eeeeee; -fx-border-width: 1 0 0 0;"
        );
        return pane;
    }



    private void handlePlaceOrder() {
        Product selected = catalogTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showOrderError("Please select a product from the catalog."); return; }
        int qty = qtySpinner.getValue();
        hideOrderError();

        Task<Order> task = new Task<>() {
            @Override protected Order call() throws Exception {
                return orderService.placeOrder(currentUser.getId(), selected.getId(), qty);
            }
        };

        orderSpinner.visibleProperty().bind(task.runningProperty());
        orderSpinner.managedProperty().bind(task.runningProperty());
        placeOrderBtn.disableProperty().bind(task.runningProperty());

        task.setOnSucceeded(ev -> {
            AlertHelper.showInfo("✅  Order #" + task.getValue().getId() + " placed successfully!");
            loadCatalog();
            loadActiveDeliveries();
        });
        task.setOnFailed(ev -> showOrderError(task.getException().getMessage()));
        TaskRunner.run(task);
    }



    private void loadActiveDeliveries() {
        Task<List<Order>> task = new Task<>() {
            @Override protected List<Order> call() throws Exception {
                return orderService.getOrdersForBuyer(currentUser.getId());
            }
        };
        task.setOnSucceeded(ev -> {
            List<Order> orders = task.getValue();
            deliveryContainer.getChildren().clear();
            if (orders.isEmpty()) {
                Label empty = new Label("No orders yet.");
                empty.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px;");
                deliveryContainer.getChildren().add(empty);
                return;
            }
            for (Order order : orders) {
                addOrderRow(order);
                if (order.getStatus() == OrderStatus.SHIPPED
                        && !activeDeliveries.contains(order.getId())) {
                    startDeliverySimulation(order);
                }
            }
        });
        task.setOnFailed(ev ->
                AlertHelper.showError("Failed to load orders: " + task.getException().getMessage()));
        TaskRunner.run(task);
    }

    private void addOrderRow(Order order) {

        String badgeColor = switch (order.getStatus()) {
            case PENDING   -> "#fff8e1";
            case SHIPPED   -> "#e8f5e9";
            case DELIVERED -> "#e3f2fd";
        };
        String badgeText = switch (order.getStatus()) {
            case PENDING   -> "🟡 Pending";
            case SHIPPED   -> "🟢 Shipped";
            case DELIVERED -> "✅ Delivered";
        };

        Label info = new Label(String.format("Order #%d  —  %s  ×%d  (₹%.2f)",
                order.getId(),
                order.getProductName() != null ? order.getProductName() : "Product #" + order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice()));
        info.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #212121;");

        Label statusBadge = new Label(badgeText);
        statusBadge.setStyle(
                "-fx-background-color: " + badgeColor + "; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; " +
                        "-fx-padding: 3 10 3 10; -fx-background-radius: 20;"
        );

        HBox topRow = new HBox(10, info, statusBadge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(order.getStatus() == OrderStatus.DELIVERED ? 1.0 : 0.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("progress-bar");
        bar.setUserData(order.getId());

        Label stageLabel = new Label(order.getStatus() == OrderStatus.DELIVERED
                ? "Delivered ✅" : order.getStatus().name());
        stageLabel.getStyleClass().add("stage-label");
        stageLabel.setUserData(order.getId());

        VBox row = new VBox(8, topRow, bar, stageLabel);
        row.getStyleClass().add("delivery-row");
        deliveryContainer.getChildren().add(row);
    }

    private void startDeliverySimulation(Order order) {
        activeDeliveries.add(order.getId());
        ProgressBar bar = findProgressBar(order.getId());
        Label stageLabel = findStageLabel(order.getId());
        if (bar == null || stageLabel == null) return;

        DeliveryTask deliveryTask = new DeliveryTask(
                order.getId(), bar, stageLabel, orderService, 3000L);
        deliveryTask.setOnSucceeded(ev -> loadActiveDeliveries());
        deliveryTask.setOnFailed(ev ->
                AlertHelper.showError("Delivery simulation failed for order #" + order.getId()));
        TaskRunner.run(deliveryTask);
    }

    private ProgressBar findProgressBar(int orderId) {
        for (var node : deliveryContainer.getChildren()) {
            if (node instanceof VBox row) {
                for (var child : row.getChildren()) {
                    if (child instanceof ProgressBar pb
                            && Integer.valueOf(orderId).equals(pb.getUserData())) return pb;
                }
            }
        }
        return null;
    }

    private Label findStageLabel(int orderId) {
        for (var node : deliveryContainer.getChildren()) {
            if (node instanceof VBox row) {
                for (var child : row.getChildren()) {
                    if (child instanceof Label lbl
                            && Integer.valueOf(orderId).equals(lbl.getUserData())) return lbl;
                }
            }
        }
        return null;
    }



    private void loadCatalog() {
        Task<List<Product>> task = new Task<>() {
            @Override protected List<Product> call() throws Exception {
                return productService.getCatalogWithFairPrices();
            }
        };
        catalogSpinner.visibleProperty().bind(task.runningProperty());
        catalogSpinner.managedProperty().bind(task.runningProperty());
        task.setOnSucceeded(ev -> {
            allProducts.setAll(task.getValue());
            refreshCategoryFilter();
            applyFilters();
        });
        task.setOnFailed(ev ->
                AlertHelper.showError("Failed to load catalog: " + task.getException().getMessage()));
        TaskRunner.run(task);
    }

    private void applyFilters() {
        String query    = searchField.getText();
        String category = categoryFilter.getValue();
        filteredProducts.setPredicate(product -> {
            List<Product> afterSearch   = CatalogFilter.search(List.of(product), query);
            List<Product> afterCategory = CatalogFilter.filterByCategory(afterSearch, category);
            return !afterCategory.isEmpty();
        });
    }

    private void refreshCategoryFilter() {
        String current = categoryFilter.getValue();
        Set<String> cats = new LinkedHashSet<>();
        cats.add("All");
        allProducts.forEach(p -> cats.add(p.getCategory()));
        categoryFilter.getItems().setAll(cats);
        if (current != null && cats.contains(current)) categoryFilter.setValue(current);
    }



    private void startPriceUpdater() {
        priceUpdaterFuture = TaskRunner.getScheduler().scheduleAtFixedRate(
                () -> TaskRunner.run(new PriceUpdaterTask(productService, allProducts)),
                30, 30, TimeUnit.SECONDS
        );
    }



    private void connectNotificationClient() {
        notificationClient = new NotificationClient(currentUser.getId());

        notificationClient.addListener(new NotificationClient.NotificationListener() {

            @Override
            public void onOrderShipped(int orderId, int buyerId) {

                if (buyerId != currentUser.getId()) return;
                javafx.application.Platform.runLater(() -> {

                    showSocketNotification("🚚  Order #" + orderId + " has been SHIPPED!");

                    loadActiveDeliveries();
                });
            }

            @Override
            public void onOrderDelivered(int orderId, int buyerId) {
                if (buyerId != currentUser.getId()) return;
                javafx.application.Platform.runLater(() -> {
                    showSocketNotification("✅  Order #" + orderId + " has been DELIVERED!");
                    loadActiveDeliveries();
                });
            }

            @Override
            public void onCatalogUpdated() {
                javafx.application.Platform.runLater(() -> {

                    loadCatalog();
                });
            }
        });

        notificationClient.connect();
    }


    private void showSocketNotification(String message) {
        Label banner = new Label("🔔  " + message);
        banner.setStyle(
                "-fx-background-color: #e8f5e9; -fx-text-fill: #1b5e20; " +
                        "-fx-font-weight: bold; -fx-font-size: 13px; " +
                        "-fx-padding: 8 14 8 14; -fx-background-radius: 6; " +
                        "-fx-border-color: #43a047; -fx-border-radius: 6; -fx-border-width: 1;"
        );
        banner.setMaxWidth(Double.MAX_VALUE);
        deliveryContainer.getChildren().add(0, banner);

        // Auto-remove the banner after 5 seconds
        javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
        pause.setOnFinished(e -> deliveryContainer.getChildren().remove(banner));
        pause.play();
    }



    @SuppressWarnings("unchecked")
    private <S, T> TableColumn<S, T> col(String title, String property) {
        TableColumn<S, T> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        return col;
    }

    private void showOrderError(String msg) {
        orderErrorLabel.setText("⚠  " + msg);
        orderErrorLabel.setVisible(true);
        orderErrorLabel.setManaged(true);
    }

    private void hideOrderError() {
        orderErrorLabel.setVisible(false);
        orderErrorLabel.setManaged(false);
    }

    private void navigateToLogin() {
        if (notificationClient != null) notificationClient.disconnect();
        if (priceUpdaterFuture != null) priceUpdaterFuture.cancel(false);
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
