package com.supplymind.platform_core.common.enums;

public enum InventoryTransactionType {
    IN,      // Restocking from Suppliers
    OUT,     // Sales to Customers (This is what we forecast)
    RETURN   // Returns from Customers
}