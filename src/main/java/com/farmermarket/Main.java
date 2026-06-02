package com.farmermarket;
import com.farmermarket.dao.impl.MySQLOrderDAO;
import com.farmermarket.dao.impl.MySQLProductDAO;
import com.farmermarket.dao.impl.MySQLUserDAO;
import com.farmermarket.dao.impl.MySQLOrderDAO;
import com.farmermarket.dao.impl.MySQLProductDAO;
import com.farmermarket.dao.impl.MySQLUserDAO;
import com.farmermarket.db.DatabaseConnection;
import com.farmermarket.service.FairPriceCalculator;
import com.farmermarket.service.OrderService;
import com.farmermarket.service.ProductService;
import com.farmermarket.service.UserService;
import com.farmermarket.socket.NotificationServer;
import com.farmermarket.ui.LoginController;
import com.farmermarket.ui.util.TaskRunner;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);


    private final NotificationServer notificationServer = new NotificationServer();

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Farmer–Market Direct Trading Platform...");


        notificationServer.start();


        MySQLUserDAO    userDAO    = new MySQLUserDAO();
        MySQLProductDAO productDAO = new MySQLProductDAO();
        MySQLOrderDAO   orderDAO   = new MySQLOrderDAO();

        UserService    userService    = new UserService(userDAO);
        FairPriceCalculator calculator = new FairPriceCalculator();
        ProductService productService = new ProductService(productDAO, orderDAO, calculator);
        OrderService   orderService   = new OrderService(orderDAO, productDAO);


        productService.setNotificationServer(notificationServer);
        orderService.setNotificationServer(notificationServer);


        LoginController loginController =
                new LoginController(primaryStage, userService, productService,
                        orderService, notificationServer);

        primaryStage.setScene(loginController.buildScene());
        primaryStage.setTitle("🌾 Farmer Market — Local Direct Trading Platform");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        log.info("Application started. NotificationServer on port {}.", NotificationServer.PORT);
    }

    @Override
    public void stop() {
        log.info("Application shutting down...");
        notificationServer.stop();
        TaskRunner.shutdown();
        DatabaseConnection.close();
        log.info("Shutdown complete.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
