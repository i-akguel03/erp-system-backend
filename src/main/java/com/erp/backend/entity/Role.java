package com.erp.backend.entity;

public enum Role {
    // Vollzugriff
    ROLE_ADMIN,

    // Lesezugriff auf alle Module
    ROLE_USER,

    // Modul-spezifische Lese-Rollen
    ROLE_CUSTOMERS_READ,
    ROLE_ORDERS_READ,
    ROLE_INVOICES_READ,
    ROLE_PRODUCTS_READ,
    ROLE_SUBSCRIPTIONS_READ,
    ROLE_CONTRACTS_READ,
    ROLE_PAYMENTS_READ,
    ROLE_INVENTORY_READ,
    ROLE_OPEN_ITEMS_READ,
    ROLE_DUE_SCHEDULES_READ,
    ROLE_AUDIT_LOGS_READ,
    ROLE_VORGANGS_READ,
    ROLE_ADDRESSES_READ
}

