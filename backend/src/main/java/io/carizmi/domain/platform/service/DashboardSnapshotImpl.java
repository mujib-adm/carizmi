package io.carizmi.domain.platform.service;

import io.carizmi.domain.platform.model.DashboardSnapshotVO;
import io.carizmi.domain.platform.repository.DashboardSnapshotRepository;
import io.carizmi.framework.annotation.RepositoryOwnerFor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@RepositoryOwnerFor(DashboardSnapshotRepository.class)
public class DashboardSnapshotImpl implements DashboardSnapshot {

    private final DashboardSnapshotRepository dashboardSnapshotRepo;

    @Override
    public Optional<DashboardSnapshotVO> getSnapshot() {
        return dashboardSnapshotRepo.findById(1);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSnapshot(DashboardSnapshotVO snapshot) {
        dashboardSnapshotRepo.save(snapshot);
    }
}