package com.example.diplomproject;

public enum PaymentStatus {
    PENDING,     // ожидает подтверждения
    SUCCESS,     // успешно оплачен
    FAILED,      // ошибка при оплате
    REFUNDED     // возврат средств
}
