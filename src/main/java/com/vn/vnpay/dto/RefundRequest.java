package com.vn.vnpay.dto;

public record RefundRequest(String tranType, String orderId, String transDate, int amount, String user) {
}
