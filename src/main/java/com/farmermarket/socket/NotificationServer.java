package com.farmermarket.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class NotificationServer {

    private static final Logger log = LoggerFactory.getLogger(NotificationServer.class);


    public static final int PORT = 9090;


    private final ConcurrentHashMap<Integer, PrintWriter> clients = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread acceptThread;


    public synchronized void start() {
        if (running) return;
        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setReuseAddress(true);
            running = true;
            acceptThread = new Thread(this::acceptLoop, "notification-server-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
            log.info("NotificationServer started on port {}", PORT);
        } catch (IOException e) {
            log.error("Failed to start NotificationServer on port {}: {}", PORT, e.getMessage());
        }
    }


    public synchronized void stop() {
        running = false;
        clients.values().forEach(PrintWriter::close);
        clients.clear();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.warn("Error closing server socket: {}", e.getMessage());
        }
        log.info("NotificationServer stopped.");
    }


    public void notifyOrderShipped(int orderId, int buyerId) {
        String msg = "ORDER_SHIPPED:" + orderId + ":" + buyerId;
        sendToUser(buyerId, msg);
        log.info("Pushed ORDER_SHIPPED orderId={} to buyerId={}", orderId, buyerId);
    }


    public void notifyOrderDelivered(int orderId, int buyerId) {
        String msg = "ORDER_DELIVERED:" + orderId + ":" + buyerId;
        sendToUser(buyerId, msg);
        log.info("Pushed ORDER_DELIVERED orderId={} to buyerId={}", orderId, buyerId);
    }


    public void broadcastCatalogUpdated() {
        broadcast("CATALOG_UPDATED");
        log.info("Broadcast CATALOG_UPDATED to {} clients", clients.size());
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(
                        () -> handleClient(clientSocket),
                        "notification-client-" + clientSocket.getPort()
                );
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (IOException e) {
                if (running) {
                    log.warn("Accept error: {}", e.getMessage());
                }
            }
        }
    }


    private void handleClient(Socket socket) {
        int userId = 0;
        try {
            socket.setSoTimeout(60_000);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);


            String line = reader.readLine();
            if (line != null && line.startsWith("REGISTER:")) {
                userId = Integer.parseInt(line.substring("REGISTER:".length()).trim());
                clients.put(userId, writer);
                writer.println("WELCOME:" + userId);
                log.info("Client registered: userId={}, remote={}", userId, socket.getRemoteSocketAddress());
            }


            while (running && !socket.isClosed()) {
                try {
                    String ping = reader.readLine();
                    if (ping == null) break;
                    if ("PING".equals(ping.trim())) {
                        writer.println("PONG");
                    }
                } catch (SocketTimeoutException e) {

                    writer.println("PING");
                    if (writer.checkError()) break;
                }
            }
        } catch (IOException e) {
            log.debug("Client disconnected: userId={}, reason={}", userId, e.getMessage());
        } finally {
            if (userId > 0) {
                clients.remove(userId);
                log.info("Client unregistered: userId={}", userId);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }


    private void sendToUser(int userId, String message) {
        PrintWriter writer = clients.get(userId);
        if (writer != null) {
            writer.println(message);
            if (writer.checkError()) {
                clients.remove(userId);
                log.warn("Removed dead connection for userId={}", userId);
            }
        } else {
            log.debug("No connected client for userId={} — message queued/dropped: {}", userId, message);
        }
    }


    private void broadcast(String message) {
        clients.forEach((uid, writer) -> {
            writer.println(message);
            if (writer.checkError()) {
                clients.remove(uid);
            }
        });
    }


    public int getConnectedClientCount() {
        return clients.size();
    }
}
