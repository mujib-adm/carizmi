package org.sofumar.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sofumar.portal.data.dto.LatestPaymentDto;
import org.sofumar.portal.data.dto.PaymentDto;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.core.businesslogic.Payment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment management APIs")
@RequiredArgsConstructor
public class PaymentController {

    private final Payment payment;

    @PostMapping("/add")
    @Operation(summary = "Add a new payment")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Integer>> addPayment(@RequestBody PaymentDto requestDto) {
        return payment.addPayment(requestDto);
    }

    @PutMapping("/update")
    @Operation(summary = "Update an existing payment")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Void>> updatePayment(@RequestBody PaymentDto requestDto) {
        return payment.updatePayment(requestDto);
    }

    @DeleteMapping("/delete/{paymentID}")
    @Operation(summary = "Delete payment by ID")
    @PreAuthorize("hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_ADMIN) or hasRole(T(org.sofumar.portal.constants.RoleConstants).ROLE_MANAGER)")
    public ResponseEntity<GlobalResponse<Void>> deletePayment(@PathVariable Integer paymentID) {
        return payment.deletePayment(paymentID);
    }

    @GetMapping("/get/{paymentID}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<GlobalResponse<PaymentDto>> getPayment(@PathVariable Integer paymentID) {
        return payment.getPayment(paymentID);
    }

    @GetMapping("/search")
    @Operation(summary = "Search payments")
    public ResponseEntity<GlobalResponse<List<PaymentDto>>> searchPayments(
            @RequestParam(required = false) Integer memberID,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return payment.searchPayments(memberID, feeType, year, quarter, dateFrom, dateTo, page, size, sortField,
                sortOrder);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest payments")
    public ResponseEntity<GlobalResponse<List<LatestPaymentDto>>> getLatest(@RequestParam(defaultValue = "5") int limit) {
        return payment.getLatestPayments(limit);
    }
}