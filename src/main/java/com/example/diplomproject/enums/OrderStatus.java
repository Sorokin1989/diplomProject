package com.example.diplomproject.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    PENDING("Ожидает оплаты"),
    PAID("Оплачен"),
    COMPLETED("Завершён"),
    CANCELLED("Отменён");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
}