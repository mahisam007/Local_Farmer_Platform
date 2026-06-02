package com.farmermarket.ui;

import com.farmermarket.service.OrderService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DeliveryTask extends Task<Void> {

    private static final Logger log = LoggerFactory.getLogger(DeliveryTask.class);


    public static final String[] STAGES = {
            "Order Confirmed",
            "Processing",
            "In Transit",
            "Out for Delivery",
            "Delivered"
    };

    private final int          orderId;
    private final ProgressBar  progressBar;
    private final Label        stageLabel;
    private final OrderService orderService;
    private final long         stepIntervalMs;


    public DeliveryTask(int orderId, ProgressBar progressBar, Label stageLabel,
                        OrderService orderService, long stepIntervalMs) {
        this.orderId        = orderId;
        this.progressBar    = progressBar;
        this.stageLabel     = stageLabel;
        this.orderService   = orderService;
        this.stepIntervalMs = stepIntervalMs;
    }



    @Override
    protected Void call() throws Exception {
        log.info("Delivery simulation started for orderId={}", orderId);

        for (int i = 0; i < STAGES.length; i++) {

            Thread.sleep(stepIntervalMs);

            final int   step     = i;
            final double progress = (double) (step + 1) / STAGES.length;


            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                stageLabel.setText(STAGES[step]);
            });

            log.debug("Order {} — stage {}/{}: {}", orderId, step + 1, STAGES.length, STAGES[i]);
        }

        Platform.runLater(() -> {
            try {
                orderService.markDelivered(orderId);
                log.info("Order {} marked as DELIVERED", orderId);
            } catch (Exception e) {
                log.error("Failed to mark order {} as DELIVERED", orderId, e);
            }
        });

        return null;
    }
}
