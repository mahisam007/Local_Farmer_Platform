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


public class LoginController {

    private final Stage          stage;
    private final UserService    userService;
    private final ProductService productService;
    private final OrderService   orderService;
    private final NotificationServer notificationServer;

    private TextField     emailField;
    private PasswordField passwordField;
    private Label         errorLabel;
    private Button        loginBtn;
    private ProgressIndicator spinner;

    public LoginController(Stage stage, UserService userService,
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

        Scene scene = new Scene(root, 960, 620);
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

        Label icon = new Label("🌾");
        icon.setStyle("-fx-font-size: 64px;");

        Label appName = new Label("Farmer Market");
        appName.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold; " +
                        "-fx-text-fill: white; -fx-font-family: 'Segoe UI';"
        );

        Label tagline = new Label("Direct Trading Platform");
        tagline.setStyle(
                "-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.80);"
        );

        // Decorative divider
        Rectangle divider = new Rectangle(60, 3);
        divider.setFill(Color.web("#a5d6a7"));
        divider.setArcWidth(3);
        divider.setArcHeight(3);

        Label desc = new Label(
                "Connecting farmers directly\nwith buyers — no middlemen,\nfair prices for everyone."
        );
        desc.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.70); " +
                        "-fx-text-alignment: center; -fx-wrap-text: true;"
        );
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);


        HBox pills = new HBox(10);
        pills.setAlignment(Pos.CENTER);
        pills.getChildren().addAll(
                pill("🌱 Farmers"),
                pill("🛒 Buyers"),
                pill("📊 Fair Price")
        );

        content.getChildren().addAll(icon, appName, tagline, divider, desc, pills);
        panel.getChildren().add(content);
        return panel;
    }

    private Label pill(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-background-color: rgba(255,255,255,0.15); " +
                        "-fx-text-fill: white; -fx-font-size: 11px; " +
                        "-fx-padding: 4 10 4 10; -fx-background-radius: 20;"
        );
        return l;
    }



    private ScrollPane buildFormPanel() {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(52, 56, 52, 56));
        card.setStyle("-fx-background-color: white;");
        card.setMaxWidth(580);


        Label title = new Label("Welcome back 👋");
        title.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1b5e20;"
        );
        Label subtitle = new Label("Sign in to your account");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #9e9e9e;");


        VBox emailBox = fieldBox("Email address", "you@example.com", false);
        emailField = (TextField) ((VBox) emailBox).getChildren().get(1);


        VBox passBox = new VBox(5);
        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("form-field-label");
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passBox.getChildren().addAll(passLbl, passwordField);


        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setMaxWidth(Double.MAX_VALUE);


        loginBtn = new Button("Sign In →");
        loginBtn.getStyleClass().add("primary-button");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(44);
        loginBtn.setStyle(loginBtn.getStyle() + "-fx-font-size: 14px;");


        spinner = new ProgressIndicator();
        spinner.setPrefSize(26, 26);
        spinner.setVisible(false);
        spinner.setManaged(false);

        HBox btnRow = new HBox(12, loginBtn, spinner);
        btnRow.setAlignment(Pos.CENTER_LEFT);


        HBox divRow = new HBox(10);
        divRow.setAlignment(Pos.CENTER);
        Region l1 = new Region(); HBox.setHgrow(l1, Priority.ALWAYS);
        l1.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 1 0 0 0; -fx-pref-height: 1;");
        Label orLbl = new Label("or");
        orLbl.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 12px;");
        Region l2 = new Region(); HBox.setHgrow(l2, Priority.ALWAYS);
        l2.setStyle("-fx-border-color: #eeeeee; -fx-border-width: 1 0 0 0; -fx-pref-height: 1;");
        divRow.getChildren().addAll(l1, orLbl, l2);


        HBox regRow = new HBox(6);
        regRow.setAlignment(Pos.CENTER);
        Label noAcct = new Label("Don't have an account?");
        noAcct.setStyle("-fx-text-fill: #9e9e9e; -fx-font-size: 13px;");
        Hyperlink regLink = new Hyperlink("Create one");
        regLink.setOnAction(e -> navigateToRegister());
        regRow.getChildren().addAll(noAcct, regLink);


        passwordField.setOnAction(e -> handleLogin());
        loginBtn.setOnAction(e -> handleLogin());

        card.getChildren().addAll(
                title, subtitle,
                new Separator(),
                emailBox, passBox,
                errorLabel,
                btnRow,
                divRow,
                regRow
        );

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        HBox.setHgrow(scroll, Priority.ALWAYS);
        return scroll;
    }



    private VBox fieldBox(String label, String prompt, boolean isPassword) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-field-label");
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(lbl, tf);
        return box;
    }



    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        hideError();

        Task<User> task = new Task<>() {
            @Override protected User call() throws Exception {
                return userService.authenticate(email, password);
            }
        };

        spinner.visibleProperty().bind(task.runningProperty());
        spinner.managedProperty().bind(task.runningProperty());
        loginBtn.disableProperty().bind(task.runningProperty());

        task.setOnSucceeded(ev -> navigateToDashboard(task.getValue()));
        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            showError(ex != null ? ex.getMessage() : "Login failed. Please try again.");
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

    private void navigateToRegister() {
        RegisterController ctrl =
                new RegisterController(stage, userService, productService,
                        orderService, notificationServer);
        stage.setScene(ctrl.buildScene());
        stage.setTitle("Farmer Market — Register");
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
