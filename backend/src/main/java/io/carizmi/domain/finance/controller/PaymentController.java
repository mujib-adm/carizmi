package io.carizmi.domain.finance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.carizmi.domain.finance.data.dto.response.LatestPaymentDto;
import io.carizmi.domain.finance.data.dto.PaymentDto;
import io.carizmi.domain.finance.data.dto.request.PaymentSearchRequestDto;
import io.carizmi.framework.data.response.GlobalResponse;
import io.carizmi.framework.data.response.PagedResult;
import io.carizmi.framework.util.ResponseUtils;
import io.carizmi.domain.finance.service.Payment;
import io.carizmi.infrastructure.security.annotation.IsAuthenticated;
import io.carizmi.infrastructure.security.annotation.IsAdminOrManager;
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

import static io.carizmi.shared.message.ValidationMessages.*;

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
        Integer id = payment.addPayment(requestDto);
        return ResponseUtils.okWithData(id, RECORD_ADDED.addMessageArgs("Payment").getMessageString());
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing payment")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> updatePayment(@Valid @RequestBody PaymentDto requestDto) {
        payment.updatePayment(requestDto);
        return ResponseUtils.ok(RECORD_UPDATED.addMessageArgs("Payment").getMessageString());
    }

    @DeleteMapping("/delete/{paymentID}")
    @Operation(summary = "Delete payment by ID")
    @IsAdminOrManager
    public ResponseEntity<GlobalResponse<Void>> deletePayment(@PathVariable @NonNull Integer paymentID) {
        payment.deletePayment(paymentID);
        return ResponseUtils.ok(RECORD_DELETED.addMessageArgs("Payment").getMessageString());
    }

    @GetMapping("/get/{paymentID}")
    @Operation(summary = "Get payment by ID")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<PaymentDto>> getPayment(@PathVariable @NonNull Integer paymentID) {
        return ResponseUtils.okWithData(payment.getPayment(paymentID));
    }

    @PostMapping("/search")
    @Operation(summary = "Search payments")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<PaymentDto>>> searchPayments(
            @RequestBody PaymentSearchRequestDto request) {
        PagedResult<PaymentDto> result = payment.searchPayments(request);
        return ResponseUtils.okWithDataPageable(result.items(), result.meta());
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest payments")
    @IsAuthenticated
    public ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> latestPayments(@RequestParam(defaultValue = "5") int limit) {
        return ResponseUtils.okWithData(payment.getLatestPayments(limit));
    }
}