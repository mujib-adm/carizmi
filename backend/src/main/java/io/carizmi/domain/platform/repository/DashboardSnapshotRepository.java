package io.carizmi.domain.platform.repository;

import io.carizmi.domain.platform.model.DashboardSnapshotVO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardSnapshotRepository extends JpaRepository<DashboardSnapshotVO, Integer> {
}