package org.sofumar.portal.core.businesslogic.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.core.businesslogic.Expense;
import org.sofumar.portal.data.dto.ExpenseDto;
import org.sofumar.portal.data.dto.request.ExpenseSearchRequestDto;
import org.sofumar.portal.data.transformer.ExpenseDtoTransformer;
import org.sofumar.portal.data.transformer.ExpenseVOTransformer;
import org.sofumar.portal.core.vo.ExpenseVO;
import org.sofumar.portal.framework.data.response.PagedResult;
import org.sofumar.portal.framework.data.response.PaginationMeta;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.core.repo.ExpenseRepository;
import org.sofumar.portal.core.repo.jpaspec.ExpenseSpecifications;
import org.sofumar.portal.service.validation.ExpenseValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.sofumar.portal.message.ValidationMessages.RECORD_NOT_FOUND;

@Service
public non-sealed class ExpenseImpl extends ExpenseAbstractBL implements Expense {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseImpl.class);

    private final ExpenseVOTransformer voTransformer;
    private final ExpenseDtoTransformer dtoTransformer;
    private final ExpenseValidator validator;

    @Autowired
    public ExpenseImpl(ExpenseRepository expenseRepo, ExpenseVOTransformer voTransformer,
                       ExpenseDtoTransformer dtoTransformer, ExpenseValidator validator) {
        super(expenseRepo);
        this.voTransformer = voTransformer;
        this.dtoTransformer = dtoTransformer;
        this.validator = validator;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected void performDomainValidation(ExpenseVO vo, boolean isUpdate) {
        if (isUpdate) {
            validator.validateForUpdate(vo);
        } else {
            validator.validate(vo);
        }
    }

    @Override
    @Transactional
    public Integer addExpense(ExpenseDto requestDto) {
        ExpenseVO vo = voTransformer.transform(requestDto);
        ExpenseVO savedExpense = add(vo);
        return savedExpense.getExpenseID();
    }

    @Override
    @Transactional
    public void updateExpense(ExpenseDto requestDto) {
        logger.info("Updating expense: {}", requestDto.getExpenseID());

        ExpenseVO existing = getRepo().findById(requestDto.getExpenseID())
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));

        ExpenseVO updated = voTransformer.transformForUpdate(requestDto, existing);
        update(updated);
    }

    @Override
    @Transactional
    public void deleteExpense(@NonNull Integer expenseID) {
        ExpenseVO existing = getRepo().findById(expenseID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseDto getExpense(@NonNull Integer expenseID) {
        ExpenseVO existing = getRepo().findById(expenseID)
                .orElseThrow(() -> new RecordNotFoundException(RECORD_NOT_FOUND.getMessageText()));
        return dtoTransformer.transform(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<ExpenseDto> searchExpenses(ExpenseSearchRequestDto request) {

        List<Specification<ExpenseVO>> specs = new ArrayList<>();
        if (request.getCategory() != null)
            specs.add(ExpenseSpecifications.hasCategory(request.getCategory()));
        if (request.getDateFrom() != null || request.getDateTo() != null)
            specs.add(ExpenseSpecifications.dateOfExpenseBetween(request.getDateFrom(), request.getDateTo()));

        Specification<ExpenseVO> spec = Specification.allOf(specs);
        Page<ExpenseVO> result = getRepo().findAll(spec, request.toPageable());
        PaginationMeta meta = PaginationMeta.of(result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());

        return PagedResult.of(dtoTransformer.transformList(result.toList()), meta);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountByDateOfExpenseBetween(LocalDate start, LocalDate end) {
        return getRepo().sumAmountByDateOfExpenseBetween(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumAmountByDateOfExpenseBefore(LocalDate date) {
        return getRepo().sumAmountByDateOfExpenseBefore(date);
    }
}