Local Farmer–Market Direct Trading Platform (JavaFX)

A GUI-based direct trade platform matching local farmers with buyers, implemented in Java with JavaFX, JDBC persistence, and a multi-threaded TCP notification server.
This application connects farmers and consumers directly to ensure fair crop pricing, bypass middleman fees, and manage delivery logistics in real time.

Features

* Modern JavaFX Interface — secure login/registration windows, role-specific layouts (Farmer, Buyer, Delivery Person), interactive catalog tables, and live order tracking trackers.
* Automated Valuation Engine — the FairPriceCalculator analyzes immediate market supply and demand trends to adjust crop pricing metrics dynamically.
* Multithreading — the background notification server runs on its own thread, periodic price monitors auto-refresh the catalog every 30 seconds, long-running delivery tasks simulate transit progress asynchronously, and all core thread execution loops are handled cleanly by a central 4-thread Executor Service.
* Core Security Tier — user authentication records are fully protected via cryptographic BCrypt password hashing algorithms at registration.
* Relational JDBC Persistence (MySQL) — structured tables managing users, products, and order data logs with explicit foreign keys, cascade safety rules, and database-level check invariants.
* TCP Notification Server (java.net) — a live network socket broadcaster operating on port 9090 that manages active connection nodes via a keep-alive ping-pong heartbeat protocol and instantly pushes transactional alerts.
* Structured Logging — utilizing an SLF4J / Logback system to stream detailed, thread-explicit console diagnostics capturing active execution contexts.

Project structure

src/main/
├── java/com/farmermarket/
│   ├── Main.java                                # entry point (wires dependency injection and starts stage)
│   ├── db/
│   │   └── DatabaseConnection.java              # connection infrastructure pool manager (HikariCP)
│   ├── model/
│   │   ├── User.java                            # user account properties model
│   │   ├── Role.java                            # ENUM: FARMER | BUYER | DELIVERY_PERSON
│   │   ├── Product.java                         # catalog item properties model with dynamic fairPrice field
│   │   ├── Order.java                           # transaction tracking data properties model
│   │   └── OrderStatus.java                     # ENUM: PENDING | SHIPPED | DELIVERED
│   ├── dao/
│   │   ├── UserDAO.java                         # data persistence contract interface
│   │   ├── ProductDAO.java                      # data persistence contract interface
│   │   ├── OrderDAO.java                        # data persistence contract interface
│   │   ├── exception/
│   │   │   ├── FarmerMarketException.java       # root custom unchecked runtime exception
│   │   │   ├── DataAccessException.java         # safety wrapper encapsulating raw SQLExceptions
│   │   │   ├── ValidationException.java         # catches form input constraint failures
│   │   │   └── AuthenticationException.java     # handles login credential mismatches
│   │   └── impl/
│   │       ├── MySQLUserDAO.java                # concrete relational storage engine
│   │       ├── MySQLProductDAO.java             # inventory manager with concurrency-safe stock decrement controls
│   │       └── MySQLOrderDAO.java               # optimized relational JOIN queries for dashboard feeds
│   ├── service/
│   │   ├── UserService.java                     # account management and secure BCrypt hashing coordinator
│   │   ├── ProductService.java                  # catalog updates and dynamic valuation controller
│   │   ├── OrderService.java                    # transactional order checkout processor
│   │   └── FairPriceCalculator.java             # pure supply and demand elasticity computation engine
│   ├── socket/
            NotificationClient.java
│   │   └── NotificationServer.java              # multi-threaded TCP event broadcaster and client map manager
│   └── ui/
│       ├── LoginController.java                 # identity verification presentation controller
│       ├── RegisterController.java              # account setup presentation controller
│       ├── FarmerDashboardController.java       # crop inventory controls and farmer sales logs
│       ├── BuyerDashboardController.java        # active marketplace catalog viewer and order forms
│       ├── DeliveryTask.java                    # background thread timeline simulator (5-stage dispatch)
│       ├── PriceUpdaterTask.java                # 30-second background catalog synchronization worker
│       └── util/
│           ├── TaskRunner.java                  # dedicated thread pool execution manager (ExecutorService)
│           ├── AlertHelper.java                 # thread-safe JavaFX graphical dialog message windows
│           └── CatalogFilter.java               # isolated search matching and sorting filters
└── resources/
    ├── db.properties                            # environment database credentials configuration file
    ├── schema.sql                               # relational MySQL DDL layout scripts
    ├── logback.xml                              # thread-explicit pattern printing configuration
    └── com/farmermarket/
        └── styles.css                           # unified graphical presentation stylesheet (green palette)

How it works

The application operates a synchronized, multi-layered layout where user tasks trigger simultaneous updates across a local database and an active background network cluster.

Here is exactly how the backend packages interact behind the scenes:

1. Long-Lived Socket Handshake and Registry
   * When any user signs into the JavaFX app, a background task runner establishes a persistent TCP connection to the NotificationServer on Port 9090.
   * The client sends a specific command packet: "REGISTER:userId". 
   * The server's accept thread picks up the connection, maps the output pipeline into a thread-safe ConcurrentHashMap, and returns a "WELCOME:userId" confirmation. This channel remains open to receive live pushes.

2. TCP Keep-Alive Heartbeat Protocol
   * To prevent firewalls or operating system timeouts from dropping idle socket lines, a background polling mechanism monitors the network channels.
   * If an active socket stays quiet for 60 seconds, a timeout flag triggers a network "PING" check. The client immediately returns a "PONG" response packet to confirm the thread pipeline is awake.

3. Safe Transaction Processing and Concurrency Control
   * When a buyer enters their home shipping address and phone number on the checkout panel and hits buy, the task is safely passed to the OrderService on a background thread.
   * The OrderService updates the system through the MySQLProductDAO, which uses an atomic SQL statement (WHERE quantity >= ?) to decrement the stock numbers. This database lock ensures that if multiple buyers click buy on the same head of cabbage at the exact same millisecond, overselling is physically impossible.

4. Pushing Real-Time Live Alerts
   * The instant the database successfully logs the new order, the OrderService alerts the NotificationServer.
   * The server immediately uses its active connection memory map to find the specific farmer who owns the crop, and sends out network messages like "CATALOG_UPDATED" or "ORDER_SHIPPED".
   * The background thread running inside the farmer's JavaFX window intercepts this text string, and passes the refresh command to Platform.runLater(). This updates their dashboard tables instantly without making them click a manual reload button.



Inspecting the database

The application stores data in your local MySQL instance. You can explore the live changes using any database manager UI (like MySQL Workbench), IntelliJ's built-in Database window tool panel, or your terminal CLI client:

USE farmermarket;
SELECT * FROM users;
SELECT * FROM products WHERE quantity > 0;
SELECT * FROM orders ORDER BY id DESC LIMIT 10;

Using the GUI

1. Log in or create a new profile. Your landing dashboard layout dynamically adjusts based on the account type you choose (FARMER, BUYER, or DELIVERY_PERSON).
2. Farmer View — Use the input forms to add crops to the catalog with names, categories, initial quantities, and base prices. Your active inventory tables will track sales automatically.
3. Buyer View — Browse available items in the marketplace table. To make a purchase, type in your delivery home address and telephone contact number, then click checkout.
4. Delivery View — Open this dashboard to view pending delivery manifests. Drivers can click on an active order to claim it, which changes its status to SHIPPED and triggers an instant background simulation that tracks delivery progress step-by-step.
5. All background network updates, dynamic price fluctuations, and transactional order details will display across your log panels and dashboards in real time.

Notes

* The platform utilizes a 4-thread ExecutorService pool inside TaskRunner to offload heavy database queries and network processing tasks away from the main JavaFX application loop, keeping your UI responsive.
* Password credentials are fully protected; the system never saves raw text passwords, storing only secure BCrypt hashes inside the users table.
* The application logging subsystem is explicitly configured inside logback.xml to print the thread name tag context (e.g., [MarketNotificationServer] or [FX-Background-Worker]) alongside every event trace.
