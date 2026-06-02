# Local Farmer–Market Direct Trading Platform

A modern, multi-threaded JavaFX desktop application designed to bridge the gap between local farmers and buyers. By eliminating intermediate brokers, this platform empowers farmers to list fresh produce directly, manage inventory levels, and engage in secure, real-time trading with retail consumers and bulk buyers within a localized marketplace ecosystem.



Core Features

Direct Marketplace Trading:** Facilitates high-throughput transaction channels between agricultural producers and buyers with robust real-time tracking.
Asynchronous Notification Engine:** Utilizes a dedicated, multi-threaded TCP backend server infrastructure to handle instant transaction alerts, message broadcasts, and system updates without interrupting UI performance.
Decoupled Database Architecture:** Leverages a strict Data Access Object (DAO) pattern to abstract core business domain spaces completely from underlying low-level persistent SQL queries.
Secure Authentication Middleware:** Tailored validation structures check data integrity, intercept illegal application paths, verify session roles (Farmer vs. Buyer), and secure credentials.



 Project File Structure

The codebase is organized following decoupled separation of concerns, keeping the user interface, backend data mapping layer, and custom messaging socket layer fully isolated:

text
Local_Farmer_Platform/
│
├── .idea/                             # IntelliJ project configuration metadata
├── .gitignore                         # Specifies intentionally untracked files to ignore
├── pom.xml                            # Maven project object model dependency mappings
└── src/
    └── main/
        └── java/
            └── com/
                └── farmermarket/
                    ├── db/
                    │   └── DatabaseConnection.java       # Centralized JDBC connection manager
                    │
                    ├── dao/
                    │   ├── exception/                    # Native runtime exceptions
                    │   │   ├── AuthenticationException.java
                    │   │   ├── DataAccessException.java
                    │   │   ├── FarmerMarketException.java
                    │   │   └── ValidationException.java
                    │   │
                    │   ├── impl/                         # Concrete MySQL persistence drivers
                    │   │   ├── MySQLOrderDAO.java
                    │   │   ├── MySQLProductDAO.java
                    │   │   └── MySQLUserDAO.java
                    │   │
                    │   ├── OrderDAO.java                 # Abstract domain CRUD interfaces
                    │   ├── ProductDAO.java
                    │   └── UserDAO.java
                    │
                    ├── model/                            # Data entities / POJOs
                    │   ├── Order.java
                    │   ├── OrderStatus.java              # Enum for tracking transaction state
                    │   ├── Product.java
                    │   ├── Role.java                     # Enum differentiating FARMER / BUYER
                    │   └── User.java
                    │
                    ├── socket/                           # Multi-threaded alert engine
                    │   ├── NotificationClient.java       # Asynchronous background daemon connection
                    │   └── NotificationServer.java       # High-concurrency TCP broadcast server
                    │
                    └── ui/
                        └── util/
                            └── TaskRunner.java           # Thread pool manager for JavaFX workers
  How It Works
The platform operates as a distributed system where the JavaFX desktop client framework interacts concurrently with a relational MySQL database (data persistence state) and an active messaging network server socket (UI mutation state).
┌────────────────────────────────────────────────────────┐
│                  JavaFX Client GUI                     │
└───────────┬────────────────────────────────┬───────────┘
            │                                │
            │ (1) CRUD Queries               │ (3) Persistent Connection
            ▼                                ▼
┌───────────────────────┐        ┌───────────────────────┐
│     MySQL Database    │        │  NotificationServer   │
│   (via DAO Pattern)   │        │     (Port 9090)       │
└───────────────────────┘        └───────────────────────┘
            ▲                                │
            │                                │
            └────── (2) Broadcast Triggers ──┘
Tech Stack & Dependencies
Frontend Framework: JavaFX (Modern native desktop UI/UX components)

Backend Core Architecture: Java SE (Multi-threading primitives, Streams API, TCP/IP Networking Sockets)

Database Management System: MySQL Server (Relational storage schema)

Build Automation & Dependency Manager: Apache Maven

Logging Interface: SLF4J / Logback configuration factory
Design Patterns Implemented
Data Access Object (DAO) Pattern: Structural abstractions isolate business processing boundaries completely from low-level infrastructure dependencies. Business rules depend on interfaces like ProductDAO, making data providers fully hot-swappable.

Socket-Based Client-Server Architecture: Centralized ServerSocket engines (NotificationServer) utilize execution loop blocks to wait for incoming connections and seamlessly dispatch client handshakes to decoupled worker runner contexts.

Custom Exception Hierarchy: Eliminates ambiguous bubbling bugs by trapping core operations within domain-specific runtime error containers (DataAccessException).
