package com.farmermarket.ui;

import com.farmermarket.model.Role;
import com.farmermarket.model.User;
import com.farmermarket.service.OrderService;
import com.farmermarket.service.ProductService;
import com.farmermarket.service.UserService;
import com.farmermarket.socket.NotificationServer;
import com.farmermarket.ui.util.TaskRunner;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;


public class RegisterController {

    private final Stage          stage;
    private final UserService    userService;
    private final ProductService productService;
    private final OrderService   orderService;
    private final NotificationServer notificationServer;

    private TextField     nameField;
    private TextField     emailField;
    private PasswordField passwordField;
    private ToggleGroup   roleToggle;
    private Label         errorLabel;
    private Button        registerBtn;
    private ProgressIndicator spinner;

    public RegisterController(Stage stage, UserService userService,
                              ProductService productService, OrderService orderService,
                              NotificationServer notificationServer) {
        this.stage               = stage;
        this.userService         = userService;
        this.productService      = productService;
        this.orderService        = orderService;
        this.notificationServer  = notificationServer;
    }



    public Scene buildScene() {
        HBox root = new HBox();
        root.setMinSize(900, 600);
        root.getChildren().addAll(buildBrandPanel(), buildFormPanel());

        Scene scene = new Scene(root, 960, 660);
        applyStylesheet(scene);
        return scene;
    }


    private StackPane buildBrandPanel() {
        StackPane panel = new StackPane();
        panel.setPrefWidth(380);
        panel.setMinWidth(380);
        panel.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #1b5e20, #2e7d32, #43a047);"
        );

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(48));

        Label icon = new Label("🌱");
        icon.setStyle("-fx-font-size: 64px;");

        Label appName = new Label("Join the Platform");
        appName.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold; " +
                        "-fx-text-fill: white; -fx-font-family: 'Segoe UI';"
        );

        Rectangle divider = new Rectangle(60, 3);
        divider.setFill(Color.web("#a5d6a7"));
        divider.setArcWidth(3);
        divider.setArcHeight(3);

        Label desc = new Label(
                "Register as a Farmer to list\nyour produce, or as a Buyer\nto shop directly from farms."
        );
        desc.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.75); " +
                        "-fx-text-alignment: center; -fx-wrap-text: true;"
        );
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);


        VBox benefits = new VBox(10);
        benefits.setAlignment(Pos.CENTER_LEFT);
        benefits.setPadding(new Insets(10, 0, 0, 0));
        benefits.getChildren().addAll(
                benefit("✅  No middlemen — direct trade"),
                benefit("✅  Fair Price Calculator"),
                benefit("✅  Real-time delivery tracking"),
                benefit("✅  Live market price updates")
        );

        content.getChildren().addAll(icon, appName, divider, desc, benefits);
        panel.getChildren().add(content);
        return panel;
    }

    private Label benefit(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: rgba(255,255,255,0.80); -fx-font-size: 12px;");
        return l;
    }



    private ScrollPane buildFormPanel() {
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(44, 56, 44, 56));
        card.setStyle("-fx-background-color: white;");
        card.setMaxWidth(580);

        Label title = new Label("Create your account");
        title.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;"
        );
        Label subtitle = new Label("It's free and takes less than a minute");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #9e9e9e;");

        // Name
        VBox nameBox = new VBox(5);
        Label nameLbl = new Label("Full Name");
        nameLbl.getStyleClass().add("form-field-label");
        nameField = new TextField();
        nameField.setPromptText("Your full name");
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameBox.getChildren().addAll(nameLbl, nameField);

        // Email
        VBox emailBox = new VBox(5);
        Label emailLbl = new Label("Email Address");
        emailLbl.getStyleClass().add("form-field-label");
        emailField = new TextField();
        emailField.setPromptText("you@example.com");
        emailField.setMaxWidth(Double.MAX_VALUE);
        emailBox.getChildren().addAll(emailLbl, emailField);

        // Password
        VBox passBox = new VBox(5);
        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("form-field-label");
        passwordField = new PasswordField();
        passwordField.setPromptText("Choose a strong password");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passBox.getChildren().addAll(passLbl, passwordField);

        // Role selection — card-style toggle
        VBox roleBox = new VBox(8);
        Label roleLbl = new Label("I want to...");
        roleLbl.getStyleClass().add("form-field-label");

        roleToggle = new ToggleGroup();

        ToggleButton farmerToggle = new ToggleButton("🌾  I'm a Farmer");
        farmerToggle.setToggleGroup(roleToggle);
        farmerToggle.setUserData(Role.FARMER);
        farmerToggle.setSelected(true);
        farmerToggle.setMaxWidth(Double.MAX_VALUE);
        styleRoleToggle(farmerToggle);

        ToggleButton buyerToggle = new ToggleButton("🛒  I'm a Buyer");
        buyerToggle.setToggleGroup(roleToggle);
        buyerToggle.setUserData(Role.BUYER);
        buyerToggle.setMaxWidth(Double.MAX_VALUE);
        styleRoleToggle(buyerToggle);

        ToggleButton deliveryToggle = new ToggleButton("🚚  Delivery Person");
        deliveryToggle.setToggleGroup(roleToggle);
        deliveryToggle.setUserData(Role.DELIVERY_PERSON);
        deliveryToggle.setMaxWidth(Double.MAX_VALUE);
        styleRoleToggle(deliveryToggle);

        // Update styles when selection changes
        roleToggle.selectedToggleProperty().addListener((obs, old, sel) -> {
            styleRoleToggle(farmerToggle);
            styleRoleToggle(buyerToggle);
            styleRoleToggle(deliveryToggle);
        });

        VBox roleRow = new VBox(8);
        HBox topRoleRow = new HBox(10, farmerToggle, buyerToggle);
        HBox.setHgrow(farmerToggle, Priority.ALWAYS);
        HBox.setHgrow(buyerToggle, Priority.ALWAYS);
        deliveryToggle.setMaxWidth(Double.MAX_VALUE);
        roleRow.getChildren().addAll(topRoleRow, deliveryToggle);
        roleBox.getChildren().addAll(roleLbl, roleRow);

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        // Register button
        registerBtn = new Button("Create Account →");
        registerBtn.getStyleClass().add("primary-button");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setPrefHeight(44);

        spinner = new ProgressIndicator();
        spinner.setPrefSize(26, 26);
        spinner.setVisible(false);
        spinner.setManaged(false);

        HBox btnRow = new HBox(12, registerBtn, spinner);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        // Back to login
        HBox loginRow = new HBox(6);
        loginRow.setAlignment(Pos.CENTER);
        Label hasAcct = new Label("Already have an account?");
        hasAcct.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px;");
        Hyperlink loginLink = new Hyperlink("Sign in");
        loginLink.setOnAction(e -> navigateToLogin());
        loginRow.getChildren().addAll(hasAcct, loginLink);

        registerBtn.setOnAction(e -> handleRegister());

        card.getChildren().addAll(
                title, subtitle,
                new Separator(),
                nameBox, emailBox, passBox,
                roleBox,
                errorLabel,
                btnRow,
                loginRow
        );

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        HBox.setHgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private void styleRoleToggle(ToggleButton btn) {
        boolean selected = btn.isSelected();
        if (selected) {
            btn.setStyle(
                    "-fx-background-color: #e8f5e9; " +
                            "-fx-border-color: #43a047; -fx-border-width: 2; " +
                            "-fx-border-radius: 8; -fx-background-radius: 8; " +
                            "-fx-text-fill: #1b5e20; -fx-font-weight: bold; " +
                            "-fx-font-size: 13px; -fx-padding: 10 16 10 16; -fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                    "-fx-background-color: #f5f5f5; " +
                            "-fx-border-color: #e0e0e0; -fx-border-width: 1.5; " +
                            "-fx-border-radius: 8; -fx-background-radius: 8; " +
                            "-fx-text-fill: #424242; -fx-font-size: 13px; " +
                            "-fx-padding: 10 16 10 16; -fx-cursor: hand;"
            );
        }
    }



    private void handleRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        Role   role     = (Role) roleToggle.getSelectedToggle().getUserData();
        hideError();

        Task<User> task = new Task<>() {
            @Override protected User call() throws Exception {
                return userService.register(name, email, password, role);
            }
        };

        spinner.visibleProperty().bind(task.runningProperty());
        spinner.managedProperty().bind(task.runningProperty());
        registerBtn.disableProperty().bind(task.runningProperty());

        task.setOnSucceeded(ev -> navigateToDashboard(task.getValue()));
        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            showError(ex != null ? ex.getMessage() : "Registration failed. Please try again.");
        });

        TaskRunner.run(task);
    }



    private void navigateToDashboard(User user) {
        if (user.getRole() == Role.FARMER) {
            FarmerDashboardController ctrl =
                    new FarmerDashboardController(stage, user, productService, orderService,
                            userService, notificationServer);
            stage.setScene(ctrl.buildScene());
        } else if (user.getRole() == Role.DELIVERY_PERSON) {
            DeliveryPersonDashboardController ctrl =
                    new DeliveryPersonDashboardController(stage, user, orderService,
                            productService, userService,
                            notificationServer);
            stage.setScene(ctrl.buildScene());
        } else {
            BuyerDashboardController ctrl =
                    new BuyerDashboardController(stage, user, productService, orderService,
                            userService, notificationServer);
            stage.setScene(ctrl.buildScene());
        }
        stage.setTitle("Farmer Market — Dashboard");
    }

    private void navigateToLogin() {
        LoginController ctrl =
                new LoginController(stage, userService, productService,
                        orderService, notificationServer);
        stage.setScene(ctrl.buildScene());
        stage.setTitle("Farmer Market — Login");
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void applyStylesheet(Scene scene) {
        var css = getClass().getResource("/com/farmermarket/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
    }
}
