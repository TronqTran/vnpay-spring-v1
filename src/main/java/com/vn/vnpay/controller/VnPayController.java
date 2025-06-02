package com.vn.vnpay.controller;

import com.vn.vnpay.dto.RefundRequest;
import com.vn.vnpay.dto.VnPayRequest;
import com.vn.vnpay.service.VnPayService;
import com.vn.vnpay.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vn-pay")
@RequiredArgsConstructor
public class VnPayController {
    private final VnPayService vnPayService;
    private final VnPayUtil vnPayUtil;
    @PostMapping("/create-payment")
    public ResponseEntity<Map<String, String>> createPayment(HttpServletRequest request, @RequestBody VnPayRequest vnPayRequest) {
        try {
            String clientIp = vnPayUtil.getIpAddress(request);
            Map<String, String> response = vnPayService.createPaymentUrl(vnPayRequest, clientIp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "99", "message", "Lỗi tạo URL thanh toán"));
        }
    }

    @GetMapping("/vn-pay-callback")
    public ResponseEntity<Map<String, String>> payCallbackHandler(HttpServletRequest request) {
        String status = request.getParameter("vnp_ResponseCode");
        if (status.equals("00")) {
            return ResponseEntity.ok(Map.of("code","ok", "message", "success"));
        } else {
            return ResponseEntity.ok(Map.of("code","fail", "message", "fail"));
        }
    }

    @PostMapping("/query")
    public ResponseEntity<?> queryTransaction(
            @RequestParam("order_id") String orderId,
            @RequestParam("trans_date") String transDate,
            HttpServletRequest request) {
        try {
            String clientIp = request.getRemoteAddr();
            String result = vnPayService.queryPayment(orderId, transDate, clientIp);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error querying VNPAY: " + ex.getMessage());
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<?> refund(@RequestBody RefundRequest request, HttpServletRequest servletRequest) {
        try {
            String response = vnPayService.refundTransaction(
                    request.tranType(),
                    request.orderId(),
                    request.transDate(),
                    request.amount(),
                    request.user(),
                    servletRequest.getRemoteAddr()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Refund failed: " + e.getMessage());
        }
    }
}
