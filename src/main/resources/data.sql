-- =====================================================================
-- FINAL SEED SCRIPT: SUPPLYMIND PLATFORM
-- Verified against: Warehouse.java, Product.java, Inventory.java,
--                   SalesOrder.java, PurchaseOrder.java
-- =====================================================================

-- 1. DISABLE FOREIGN KEYS (To prevent errors during insertion)
SET FOREIGN_KEY_CHECKS = 0;


-- 3. WAREHOUSES

INSERT IGNORE INTO warehouses (warehouse_id, location_name, address, capacity) VALUES
                                                                                   (1, 'Main Distribution Center', 'Montreal, QC', 10000),
                                                                                   (2, 'West Coast Hub', 'Vancouver, BC', 5000);

-- 4. CUSTOMERS

INSERT IGNORE INTO customers (customer_id, name, email, address) VALUES
                                                                     (1, 'Tech Startups Inc', 'cust1@demo.com', '101 Innovation Dr'),
                                                                     (2, 'MegaCorp Logistics', 'cust2@demo.com', '500 Enterprise Way'),
                                                                     (3, 'Gadget World', 'corp@demo.com', '888 Retail Blvd');

-- 5. SUPPLIERS

INSERT IGNORE INTO suppliers (supplier_id, name, contact_email, phone, address, is_deleted, created_at) VALUES
                                                                                                            (1, 'TechParts Inc.', 'supplymind.demo.2025+techparts@gmail.com', '555-0101', '123 Silicon Ave', 0, NOW()),
                                                                                                            (2, 'GlobalWidgets Ltd.', 'supplymind.demo.2025+widgets@gmail.com', '555-0202', '456 Industrial Blvd', 0, NOW());

-- 6. PRODUCTS

INSERT IGNORE INTO products (product_id, sku, name, category, unit_price, reorder_point, is_deleted, created_at, updated_at) VALUES
                                                                                                                                 (1, 'SKU-1001', 'Wireless Gaming Mouse', 'Electronics', 49.99, 50, 0, NOW(), NOW()),
                                                                                                                                 (2, 'SKU-1002', 'Mechanical Keyboard', 'Electronics', 89.99, 30, 0, NOW(), NOW()),
                                                                                                                                 (3, 'SKU-1003', '27-inch 4K Monitor', 'Displays', 299.99, 10, 0, NOW(), NOW()),
                                                                                                                                 (4, 'SKU-1004', 'USB-C Docking Station', 'Accessories', 129.99, 15, 0, NOW(), NOW()),
                                                                                                                                 (5, 'SKU-1005', 'Ergonomic Mesh Chair', 'Furniture', 249.99, 5, 0, NOW(), NOW());

-- 7. INVENTORY

INSERT IGNORE INTO inventory (inventory_id, warehouse_id, product_id, qty_on_hand) VALUES
                                                                                       (1, 1, 1, 45),
                                                                                       (2, 1, 2, 100),
                                                                                       (3, 1, 3, 8),
                                                                                       (4, 1, 4, 50),
                                                                                       (5, 1, 5, 20);

-- 8. SUPPLIER PRODUCTS

INSERT IGNORE INTO supplier_products (id, supplier_id, product_id, lead_time_days, cost_price) VALUES
                                                                                                   (1, 1, 1, 5, 25.00),
                                                                                                   (2, 1, 2, 7, 45.00),
                                                                                                   (3, 2, 3, 14, 150.00);

-- 9. PURCHASE ORDERS

INSERT IGNORE INTO purchase_orders (po_id, supplier_id, warehouse_id, status, total_amount, created_on) VALUES
    (1, 1, 1, 'PENDING', 2499.50, NOW() - INTERVAL 2 DAY);

-- 10. PURCHASE ORDER ITEMS

INSERT IGNORE INTO purchase_order_items (po_item_id, po_id, product_id, ordered_qty, unit_cost) VALUES
    (1, 1, 1, 50, 25.00);

-- 11. SALES ORDERS

INSERT IGNORE INTO sales_orders (so_id, customer_id, warehouse_id, status) VALUES
                                                                               (1, 1, 1, 'DELIVERED'),
                                                                               (2, 1, 1, 'DELIVERED'),
                                                                               (3, 1, 1, 'DELIVERED'),
                                                                               (4, 2, 1, 'DELIVERED'),
                                                                               (5, 2, 1, 'DELIVERED'),
                                                                               (6, 3, 1, 'DELIVERED');

-- 12. SALES ORDER ITEMS

INSERT IGNORE INTO sales_order_items (so_item_id, so_id, product_id, quantity) VALUES
                                                                                   (1, 1, 1, 10),
                                                                                   (2, 2, 1, 25),
                                                                                   (3, 3, 1, 60),
                                                                                   (4, 4, 2, 20),
                                                                                   (5, 5, 2, 22),
                                                                                   (6, 6, 3, 50);

-- 13. INVENTORY TRANSACTIONS (The "Time Machine" for AI)

INSERT IGNORE INTO inventory_transactions (transaction_id, warehouse_id, product_id, type, quantity, timestamp) VALUES
                                                                                                                    (1, 1, 1, 'SALE', 10, NOW() - INTERVAL 90 DAY),
                                                                                                                    (2, 1, 1, 'SALE', 25, NOW() - INTERVAL 60 DAY),
                                                                                                                    (3, 1, 1, 'SALE', 60, NOW() - INTERVAL 30 DAY),
                                                                                                                    (4, 1, 3, 'SALE', 50, NOW() - INTERVAL 58 DAY);

-- 14. RE-ENABLE FOREIGN KEYS
SET FOREIGN_KEY_CHECKS = 1;

-- CONFIRMATION
SELECT 'Seeding Complete' AS Status;