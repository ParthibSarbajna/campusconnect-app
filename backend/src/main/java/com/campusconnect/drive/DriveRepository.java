package com.campusconnect.drive;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DriveRepository extends JpaRepository<PlacementDrive, Long>, JpaSpecificationExecutor<PlacementDrive> {
}
