package com.farmermarket.ui;

import com.farmermarket.model.Product;
import com.farmermarket.service.ProductService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class PriceUpdaterTask extends Task<Void> {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdaterTask.class);

    private final ProductService          productService;
    private final ObservableList<Product> catalogList;


    public PriceUpdaterTask(ProductService productService,
                            ObservableList<Product> catalogList) {
        this.productService = productService;
        this.catalogList    = catalogList;
    }



    @Override
    protected Void call() {
        try {
            log.debug("Price updater cycle starting...");
            List<Product> updated = productService.getCatalogWithFairPrices();


            Platform.runLater(() -> catalogList.setAll(updated));

            log.debug("Price updater cycle complete — {} products refreshed.", updated.size());
        } catch (Exception e) {

            log.error("Price updater cycle failed — will retry on next interval.", e);
        }
        return null;
    }
}
