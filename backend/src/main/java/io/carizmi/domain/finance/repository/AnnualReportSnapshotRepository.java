package io.carizmi.domain.finance.repository;

import io.carizmi.domain.finance.model.AnnualReportSnapshotVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnualReportSnapshotRepository
        extends JpaRepository<AnnualReportSnapshotVO, Integer>, JpaSpecificationExecutor<AnnualReportSnapshotVO> {
}