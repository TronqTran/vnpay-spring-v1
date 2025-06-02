package com.vn.vnpay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vn.vnpay.config.VnPayConfig;
import com.vn.vnpay.dto.VnPayRequest;
import com.vn.vnpay.util.VnPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnPayService {
    private final VnPayConfig vnPayConfig;
    private final VnPayUtil vnPayUtil;

    public Map<String, String> createPaymentUrl(VnPayRequest request, String clientIp) {
        // Initialize payment parameters
        String transactionRef = vnPayUtil.getRandomNumber(8);
        long amountInCents = request.amount() * 100L;

        // Prepare payment parameters map
        Map<String, String> paymentParams = new HashMap<>();
        paymentParams.put("vnp_Version", vnPayConfig.getVersion());
        paymentParams.put("vnp_Command", vnPayConfig.getCommand().getPay());
        paymentParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        paymentParams.put("vnp_Amount", String.valueOf(amountInCents));
        paymentParams.put("vnp_CurrCode", vnPayConfig.getCurrCode());

        // Set bank code - default to "ncb" if not provided
        String bankCode = (request.bankCode() != null && !request.bankCode().isEmpty())
                ? request.bankCode()
                : "ncb";
        paymentParams.put("vnp_BankCode", bankCode);

        // Set order information
        paymentParams.put("vnp_TxnRef", transactionRef);
        paymentParams.put("vnp_OrderInfo", "Payment for order: " + transactionRef);
        paymentParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        paymentParams.put("vnp_Locale", "vn");
        paymentParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        paymentParams.put("vnp_IpAddr", clientIp);

        // Add timestamp information
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        paymentParams.put("vnp_CreateDate", formatter.format(calendar.getTime()));

        calendar.add(Calendar.MINUTE, 15);
        paymentParams.put("vnp_ExpireDate", formatter.format(calendar.getTime()));

        // Log the transaction reference number and creation date for payment tracking
        log.info("Transaction reference number (vnp_TxnRef)={}, Transaction creation date (vnp_CreateDate)={}, Amount={}",
                transactionRef, // Unique 8-digit transaction reference number
                paymentParams.get("vnp_CreateDate"), // Transaction timestamp in format yyyyMMddHHmmss
                request.amount()); // Transaction amount
        // Sort field names and build query string
        List<String> fieldNames = new ArrayList<>(paymentParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> iterator = fieldNames.iterator();

        // Build hash data and query string
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            String fieldValue = paymentParams.get(fieldName);

            // Append to hash data
            hashData.append(fieldName)
                    .append('=')
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            // Append to query string
            query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                    .append('=')
                    .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

            if (iterator.hasNext()) {
                hashData.append('&');
                query.append('&');
            }
        }

        // Generate and append secure hash
        String secureHash = vnPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        // Build final payment URL
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + query;

        // Create and return a response
        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        response.put("message", "success");
        response.put("code", "00");

        return response;
    }

    public String queryPayment(String orderId, String transDate, String clientIp) throws Exception {
        // Initialize request parameters
        String requestId = vnPayUtil.getRandomNumber(8);
        String version = vnPayConfig.getVersion();
        String command = vnPayConfig.getCommand().getQuery();
        String orderInfo = "Check transaction status for OrderId: " + orderId;
        String tmnCode = vnPayConfig.getTmnCode();

        // Generate current datetime in GMT+7
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(calendar.getTime());

        // Build request parameters map
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("vnp_RequestId", requestId);
        requestParams.put("vnp_Version", version);
        requestParams.put("vnp_Command", command);
        requestParams.put("vnp_TmnCode", tmnCode);
        requestParams.put("vnp_TxnRef", orderId);
        requestParams.put("vnp_OrderInfo", orderInfo);
        requestParams.put("vnp_TransactionDate", transDate);
        requestParams.put("vnp_CreateDate", createDate);
        requestParams.put("vnp_IpAddr", clientIp);

        // Generate secure hash
        String hashData = String.join("|",
                requestId, version, command, tmnCode, orderId,
                transDate, createDate, clientIp, orderInfo);
        String secureHash = vnPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        requestParams.put("vnp_SecureHash", secureHash);

        // Convert parameters to JSON
        ObjectMapper mapper = new ObjectMapper();
        String jsonRequest = mapper.writeValueAsString(requestParams);

        // Initialize HTTP connection
        URL url = new URL(vnPayConfig.getApiUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Send request
        try (DataOutputStream writer = new DataOutputStream(conn.getOutputStream())) {
            writer.writeBytes(jsonRequest);
            writer.flush();
        }

        // Read response
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    public String refundTransaction(String transactionType, String txnRef, String transDate,
                                    int amount, String username, String ipAddress) throws IOException {
        // Initialize request parameters
        Map<String, Object> requestParams = new HashMap<>();
        String requestId = vnPayUtil.getRandomNumber(8);
        long amountInCents = amount * 100L;  // Convert to VND cents
        String orderInfo = "Refund for transaction: " + txnRef;
        String transactionNo = "";

        // Get the current timestamp in GMT+7
        String createDate = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7")).getTime());

        // Build request parameters map
        requestParams.put("vnp_RequestId", requestId);
        requestParams.put("vnp_Version", vnPayConfig.getVersion());
        requestParams.put("vnp_Command", vnPayConfig.getCommand().getRefund());
        requestParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        requestParams.put("vnp_TransactionType", transactionType);
        requestParams.put("vnp_TxnRef", txnRef);
        requestParams.put("vnp_Amount", String.valueOf(amountInCents));
        requestParams.put("vnp_OrderInfo", orderInfo);
        requestParams.put("vnp_TransactionDate", transDate);
        requestParams.put("vnp_CreateBy", username);
        requestParams.put("vnp_CreateDate", createDate);
        requestParams.put("vnp_IpAddr", ipAddress);

        // Generate hash data for security
        String hashData = String.join("|",
                requestId,
                vnPayConfig.getVersion(),
                vnPayConfig.getCommand().getRefund(),
                vnPayConfig.getTmnCode(),
                transactionType,
                txnRef,
                String.valueOf(amountInCents),
                transactionNo,
                transDate,
                username,
                createDate,
                ipAddress,
                orderInfo
        );

        // Generate and append secure hash
        String secureHash = vnPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        requestParams.put("vnp_SecureHash", secureHash);

        // Convert parameters to JSON
        String jsonPayload = new ObjectMapper().writeValueAsString(requestParams);

        // Setup and configure HTTP connection
        HttpURLConnection connection = (HttpURLConnection) new URL(vnPayConfig.getApiUrl())
                .openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Send request data
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }

        // Read and return response
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        connection.getResponseCode() == 200
                                ? connection.getInputStream()
                                : connection.getErrorStream()
                ))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

}

