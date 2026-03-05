package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.response.LatestPaymentDto;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.data.dto.request.PaymentSearchRequestDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.core.businesslogic.Payment;
import org.sofumar.portal.security.annotation.IsAuthenticated;
import org.sofumar.portal.security.annotation.IsAdminOrManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.lang.NonNull;

import java.util.List;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment management APIs")
@RequiredArgsConstructor
public class PaymentController {

    private final Payment payment;

    @PostMapping("/add")
    @Operation(summary = "Add a new payment")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Integer>> addPayment(@Valid @RequestBody PaymentDto requestDto) {
        return payment.addPayment(requestDto);
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing payment")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> updatePayment(@Valid @RequestBody PaymentDto requestDto) {
        return payment.updatePayment(requestDto);
    }

    @DeleteMapping("/delete/{paymentID}")
    @Operation(summary = "Delete payment by ID")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> deletePayment(@PathVariable @NonNull Integer paymentID) {
        return payment.deletePayment(paymentID);
    }

    @GetMapping("/get/{paymentID}")
    @Operation(summary = "Get payment by ID")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<PaymentDto>> getPayment(@PathVariable @NonNull Integer paymentID) {
        return payment.getPayment(paymentID);
    }

    @PostMapping("/search")
    @Operation(summary = "Search payments")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<PaymentDto>>> searchPayments(
            @RequestBody PaymentSearchRequestDto request) {
        return payment.searchPayments(request);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest payments")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> getLatest(@RequestParam(defaultValue = "5") int limit) {
        return payment.getLatestPayments(limit);
    }
}