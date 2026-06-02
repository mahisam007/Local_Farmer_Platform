package com.farmermarket.ui;

import com.farmermarket.model.Order;
import com.farmermarket.model.OrderStatus;
import com.farmermarket.model.User;
import com.farmermarket.service.OrderService;
import com.farmermarket.service.ProductService;
import com.farmermarket.service.UserService;
import com.farmermarket.socket.NotificationServer;
import com.farmermarket.ui.util.AlertHelper;
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
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.List;


public class DeliveryPersonDashboardController {

    private final Stage          stage;
    private final User           currentUser;
    private final OrderService   orderService;
    private final ProductService productService;
    private final UserService    userService;
    private final NotificationServer notificationServer;

    private final ObservableList<Order> orderList     = FXCollections.observableArrayList();
    private       FilteredList<Order>   filteredOrders;

    private TableView<Order>   orderTable;
    private ComboBox<String>   statusFilter;
    private ProgressIndicator  tableSpinner;
    private Label              statsLabel;

    public DeliveryPersonDashboardController(Stage stage, User currentUser,
                                             OrderService orderService,
                                             ProductService productService,
                                             UserService userService,
                                             NotificationServer notificationServer) {
        this.stage               = stage;
        this.currentUser         = currentUser;
        this.orderService        = orderService;
        this.productService      = productService;
        this.userService         = userService;
        this.notificationServer  = notificationServer;
    }



    public Scene buildScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f5f5;");

        root.setTop(buildHeader());
        root.setCenter(buildMainContent());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1060, 700);
        applyStylesheet(scene);

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
                "-fx-background-color: linear-gradient(to right, #1a237e, #283593);" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.30), 10, 0, 0, 3);"
        );

        Label icon = new Label("🚚");
        icon.setStyle("-fx-font-size: 26px;");

        Label title = new Label("Delivery Dashboard");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        statsLabel = new Label("Loading...");
        statsLabel.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); " +
                        "-fx-text-fill: white; -fx-font-size: 12px; " +
                        "-fx-padding: 5 14 5 14; -fx-background-radius: 20;"
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
        logoutBtn.setOnAction(e -> navigateToLogin());

        bar.getChildren().addAll(icon, title, spacer, statsLabel, userBadge, logoutBtn);
        return bar;
    }



    private VBox buildMainContent() {
        // ── Toolbar ──
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(14, 14, 8, 14));
        toolbar.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #eeeeee transparent; -fx-border-width: 0 0 1 0;");

        Label filterLbl = new Label("Filter by status:");
        filterLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #424242; -fx-font-weight: bold;");

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("All", "PENDING", "SHIPPED", "DELIVERED");
        statusFilter.setValue("All");
        statusFilter.setPrefWidth(150);
        statusFilter.valueProperty().addListener((obs, old, val) -> applyFilter());

        tableSpinner = new ProgressIndicator();
        tableSpinner.setPrefSize(22, 22);
        tableSpinner.setVisible(false);
        tableSpinner.setManaged(false);

        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setStyle(refreshBtn.getStyle() +
                "-fx-border-color: #283593; -fx-text-fill: #283593;");
        refreshBtn.setOnAction(e -> loadOrders());


        Button markShippedBtn = new Button("🚚  Mark as SHIPPED");
        markShippedBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1976d2, #1565c0); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                        "-fx-padding: 9 18 9 18; -fx-background-radius: 6; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(21,101,192,0.40), 6, 0, 0, 2);"
        );
        markShippedBtn.setOnAction(e -> handleMarkShipped());

        Button markDeliveredBtn = new Button("✅  Mark as DELIVERED");
        markDeliveredBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #43a047, #2e7d32); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; " +
                        "-fx-padding: 9 18 9 18; -fx-background-radius: 6; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(46,125,50,0.40), 6, 0, 0, 2);"
        );
        markDeliveredBtn.setOnAction(e -> handleMarkDelivered());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(
                filterLbl, statusFilter, refreshBtn, tableSpinner,
                spacer, markShippedBtn, markDeliveredBtn
        );


        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(6, 14, 6, 14));
        legend.setStyle("-fx-background-color: #fafafa;");
        legend.getChildren().addAll(
                legendDot("#ffb300", "PENDING — awaiting pickup"),
                legendDot("#1976d2", "SHIPPED — in transit"),
                legendDot("#2e7d32", "DELIVERED — completed")
        );


        filteredOrders = new FilteredList<>(orderList, o -> true);
        orderTable = new TableView<>(filteredOrders);
        orderTable.getStyleClass().add("table-view");
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderTable.setPlaceholder(new Label("No orders found."));


        TableColumn<Order, Integer> idCol = new TableColumn<>("Order #");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(80);


        TableColumn<Order, String> buyerCol = new TableColumn<>("Buyer");
        buyerCol.setCellValueFactory(new PropertyValueFactory<>("buyerName"));


        TableColumn<Order, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(new PropertyValueFactory<>("productName"));


        TableColumn<Order, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setMaxWidth(70);


        TableColumn<Order, Double> totalCol = new TableColumn<>("Total ₹");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : String.format("₹%.2f", val));
            }
        });

        TableColumn<Order, OrderStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(OrderStatus val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setGraphic(null); setStyle(""); return; }
                String color = switch (val) {
                    case PENDING   -> "#fff8e1";
                    case SHIPPED   -> "#e3f2fd";
                    case DELIVERED -> "#e8f5e9";
                };
                String textColor = switch (val) {
                    case PENDING   -> "#f57f17";
                    case SHIPPED   -> "#1565c0";
                    case DELIVERED -> "#1b5e20";
                };
                String emoji = switch (val) {
                    case PENDING   -> "🟡";
                    case SHIPPED   -> "🔵";
                    case DELIVERED -> "✅";
                };
                Label badge = new Label(emoji + "  " + val.name());
                badge.setStyle(
                        "-fx-background-color: " + color + "; " +
                                "-fx-text-fill: " + textColor + "; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px; " +
                                "-fx-padding: 3 10 3 10; -fx-background-radius: 20;"
                );
                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER-LEFT;");
            }
        });

        orderTable.getColumns().addAll(idCol, buyerCol, prodCol, qtyCol, totalCol, statusCol);
        VBox.setVgrow(orderTable, Priority.ALWAYS);


        VBox tableCard = new VBox(orderTable);
        tableCard.setStyle(
                "-fx-background-color: white; -fx-border-color: #eeeeee; " +
                        "-fx-border-radius: 10; -fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);"
        );
        VBox.setVgrow(orderTable, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox content = new VBox(toolbar, legend, tableCard);
        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: #f5f5f5;");
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        VBox.setVgrow(content, Priority.ALWAYS);
        return content;
    }



    private HBox buildStatusBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #eeeeee transparent transparent transparent; " +
                        "-fx-border-width: 1 0 0 0;"
        );
        Label hint = new Label(
                "💡  Select an order and use the action buttons to update its status. " +
                        "Buyers receive instant push notifications on every status change."
        );
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #9e9e9e;");
        bar.getChildren().add(hint);
        return bar;
    }



    private void handleMarkShipped() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Select an order to mark as SHIPPED.");
            return;
        }
        if (selected.getStatus() != OrderStatus.PENDING) {
            AlertHelper.showError("Only PENDING orders can be marked as SHIPPED.\nSelected order is: " + selected.getStatus());
            return;
        }
        if (!AlertHelper.showConfirmation(
                "Mark Order #" + selected.getId() + " as SHIPPED?\n" +
                        "The buyer will receive an instant notification.")) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                orderService.markShipped(selected.getId());
                return null;
            }
        };
        bindSpinner(task);
        task.setOnSucceeded(ev -> {
            loadOrders();
            AlertHelper.showInfo("✅  Order #" + selected.getId() + " marked as SHIPPED.\nBuyer notified.");
        });
        task.setOnFailed(ev -> AlertHelper.showError(task.getException().getMessage()));
        TaskRunner.run(task);
    }

    private void handleMarkDelivered() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Select an order to mark as DELIVERED.");
            return;
        }
        if (selected.getStatus() != OrderStatus.SHIPPED) {
            AlertHelper.showError("Only SHIPPED orders can be marked as DELIVERED.\nSelected order is: " + selected.getStatus());
            return;
        }
        if (!AlertHelper.showConfirmation(
                "Mark Order #" + selected.getId() + " as DELIVERED?\n" +
                        "The buyer will receive an instant notification.")) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                orderService.markDelivered(selected.getId());
                return null;
            }
        };
        bindSpinner(task);
        task.setOnSucceeded(ev -> {
            loadOrders();
            AlertHelper.showInfo("✅  Order #" + selected.getId() + " marked as DELIVERED.\nBuyer notified.");
        });
        task.setOnFailed(ev -> AlertHelper.showError(task.getException().getMessage()));
        TaskRunner.run(task);
    }


    private void loadOrders() {
        Task<List<Order>> task = new Task<>() {
            @Override protected List<Order> call() throws Exception {
                return orderService.getAllOrders();
            }
        };
        bindSpinner(task);
        task.setOnSucceeded(ev -> {
            List<Order> orders = task.getValue();
            orderList.setAll(orders);
            applyFilter();
            updateStats(orders);
        });
        task.setOnFailed(ev ->
                AlertHelper.showError("Failed to load orders: " + task.getException().getMessage()));
        TaskRunner.run(task);
    }

    private void applyFilter() {
        String filter = statusFilter.getValue();
        filteredOrders.setPredicate(order -> {
            if (filter == null || filter.equals("All")) return true;
            return order.getStatus().name().equals(filter);
        });
    }

    private void updateStats(List<Order> orders) {
        long pending   = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long shipped   = orders.stream().filter(o -> o.getStatus() == OrderStatus.SHIPPED).count();
        long delivered = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        statsLabel.setText(
                "🟡 " + pending + " Pending  |  🔵 " + shipped + " Shipped  |  ✅ " + delivered + " Delivered"
        );
    }



    private HBox legendDot(String color, String label) {
        Circle dot = new Circle(5);
        dot.setFill(Color.web(color));
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #757575;");
        HBox row = new HBox(6, dot, lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void bindSpinner(Task<?> task) {
        tableSpinner.visibleProperty().bind(task.runningProperty());
        tableSpinner.managedProperty().bind(task.runningProperty());
    }

    private void navigateToLogin() {
        LoginController ctrl = new LoginController(
                stage, userService, productService, orderService, notificationServer);
        stage.setScene(ctrl.buildScene());
        stage.setTitle("Farmer Market — Login");
    }

    private void applyStylesheet(Scene scene) {
        var css = getClass().getResource("/com/farmermarket/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
    }
}
