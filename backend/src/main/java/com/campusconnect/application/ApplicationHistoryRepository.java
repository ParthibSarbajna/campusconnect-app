package com.campusconnect.application;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
}
