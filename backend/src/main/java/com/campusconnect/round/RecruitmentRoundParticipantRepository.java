package com.campusconnect.round;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitmentRoundParticipantRepository
        extends JpaRepository<RecruitmentRoundParticipant, Long> {

    List<RecruitmentRoundParticipant> findByRoundIdOrderByIdAsc(Long roundId);

    Optional<RecruitmentRoundParticipant> findByRoundIdAndApplicationId(
            Long roundId, Long applicationId);
}