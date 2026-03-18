package com.example.diplomproject.mapper;

import com.example.diplomproject.dto.PaymentDto;
import com.example.diplomproject.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentDto toPaymentDto(Payment payment) {

        if (payment == null)
            return null;

        PaymentDto paymentDto = new PaymentDto();

        paymentDto.setId(payment.getId());

        if(payment.getOrder() !=null){
        paymentDto.setOrderId(payment.getOrder().getId());
        paymentDto.setOrderStatus(String.valueOf(payment.getOrder().getOrderStatus()));
        paymentDto.setTotalSum(payment.getOrder().getTotalSum());
        }

        paymentDto.setPaymentStatus(String.valueOf(payment.getPaymentStatus()));
        paymentDto.setPaymentMethod(payment.getPaymentMethod());
        paymentDto.setTransactionId(payment.getTransactionId());
        paymentDto.setGatewayTransactionId(payment.getGatewayTransactionId());
        paymentDto.setPaymentGateway(payment.getPaymentGateway());
        paymentDto.setCurrency(payment.getCurrency());
        paymentDto.setFailureMessage(payment.getFailureMessage());

        paymentDto.setCreatedAt(payment.getCreatedAt());
        paymentDto.setUpdatedAt(payment.getUpdatedAt());

        return paymentDto;

    }
}
