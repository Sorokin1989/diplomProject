package com.example.diplomproject.enums;

public enum OrderStatus {
    PENDING("Ожидает оплаты"),
    PAID("Оплачен"),
    COMPLETED("Завершён"),
    CANCELLED("Отменён");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}