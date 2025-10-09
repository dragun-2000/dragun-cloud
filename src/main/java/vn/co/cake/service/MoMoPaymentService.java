package vn.co.cake.service;

import vn.co.cake.request.PaymentRequest;

public interface MoMoPaymentService {
    String createPaymentRequest(PaymentRequest paymentRequest) throws Exception;
}
