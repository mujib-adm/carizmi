package io.carizmi.domain.platform.service;

import io.carizmi.domain.platform.model.DashboardSnapshotVO;

import java.util.Optional;

public interface DashboardSnapshot {

    Optional<DashboardSnapshotVO> getSnapshot();

    void saveSnapshot(DashboardSnapshotVO snapshot);
}