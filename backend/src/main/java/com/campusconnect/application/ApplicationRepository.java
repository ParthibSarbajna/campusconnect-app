
package com.campusconnect.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    boolean existsByDriveIdAndStudentUserId(Long driveId, Long studentUserId);

    boolean existsByDriveId(Long driveId);

    Optional<Application> findByIdAndStudentUserId(Long id, Long studentUserId);

    Page<Application> findByStudentUserId(Long studentUserId, Pageable pageable);

    List<Application> findByStudentUserIdAndDriveIdIn(Long studentUserId, Collection<Long> driveIds);

    Optional<Application> findByIdAndDriveId(Long id, Long driveId);
}
