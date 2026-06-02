package com.farmermarket.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private static final String HOST            = "localhost";
    private static final int    PING_INTERVAL_MS = 25_000;
    private static final int    RECONNECT_DELAY_MS = 5_000;

    private final int userId;
    private volatile boolean running = false;

    private Socket       socket;
    private PrintWriter  writer;
    private Thread       readThread;
    private Thread       pingThread;

    /** Registered listeners — called when a notification arrives. */
    private final CopyOnWriteArrayList<NotificationListener> listeners =
            new CopyOnWriteArrayList<>();


    public interface NotificationListener {

        void onOrderShipped(int orderId, int buyerId);


        void onOrderDelivered(int orderId, int buyerId);


        void onCatalogUpdated();
    }


    public NotificationClient(int userId) {
        this.userId = userId;
    }


    public void connect() {
        running = true;
        readThread = new Thread(this::connectAndReadLoop, "notification-client-read-" + userId);
        readThread.setDaemon(true);
        readThread.start();
    }


    public void disconnect() {
        running = false;
        if (pingThread != null) pingThread.interrupt();
        closeSocket();
        log.info("NotificationClient disconnected for userId={}", userId);
    }



    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }


    private void connectAndReadLoop() {
        while (running) {
            try {
                log.info("NotificationClient connecting to {}:{} as userId={}",
                        HOST, NotificationServer.PORT, userId);

                socket = new Socket();
                socket.connect(new InetSocketAddress(HOST, NotificationServer.PORT), 5_000);
                socket.setSoTimeout(60_000);

                writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()), true);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));


                writer.println("REGISTER:" + userId);


                startPingThread();


                String line;
                while (running && (line = reader.readLine()) != null) {
                    handleMessage(line.trim());
                }

            } catch (IOException e) {
                if (running) {
                    log.warn("NotificationClient connection lost for userId={}: {}. Reconnecting in {}ms...",
                            userId, e.getMessage(), RECONNECT_DELAY_MS);
                    closeSocket();
                    try { Thread.sleep(RECONNECT_DELAY_MS); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
    }


    private void startPingThread() {
        if (pingThread != null && pingThread.isAlive()) pingThread.interrupt();
        pingThread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(PING_INTERVAL_MS);
                    if (writer != null) {
                        writer.println("PING");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "notification-client-ping-" + userId);
        pingThread.setDaemon(true);
        pingThread.start();
    }


    private void handleMessage(String message) {
        log.debug("NotificationClient received: {}", message);

        if (message.startsWith("ORDER_SHIPPED:")) {
            String[] parts = message.split(":");
            if (parts.length == 3) {
                int orderId = Integer.parseInt(parts[1]);
                int buyerId = Integer.parseInt(parts[2]);
                listeners.forEach(l -> l.onOrderShipped(orderId, buyerId));
            }

        } else if (message.startsWith("ORDER_DELIVERED:")) {
            String[] parts = message.split(":");
            if (parts.length == 3) {
                int orderId = Integer.parseInt(parts[1]);
                int buyerId = Integer.parseInt(parts[2]);
                listeners.forEach(l -> l.onOrderDelivered(orderId, buyerId));
            }

        } else if ("CATALOG_UPDATED".equals(message)) {
            listeners.forEach(NotificationListener::onCatalogUpdated);

        } else if ("PING".equals(message)) {

            if (writer != null) writer.println("PONG");

        } else if (message.startsWith("WELCOME:")) {
            log.info("NotificationClient registered successfully as userId={}", userId);
        }

    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
