package com.campusconnect.round;

import com.campusconnect.common.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecruitmentRoundService {

    private final RecruitmentRoundRepository rounds;

    public RecruitmentRoundService(RecruitmentRoundRepository rounds) {
        this.rounds = rounds;
    }

    public RecruitmentRound create(Long driveId, String name, Integer sequenceNumber,
                                   java.time.Instant scheduledAt, String venue,
                                   String instructions) {
        if (driveId == null || driveId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid drive ID.");
        }

        if (sequenceNumber == null || sequenceNumber <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Round sequence number must be positive.");
        }

        if (rounds.existsByDriveIdAndSequenceNumber(driveId, sequenceNumber)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A round with this sequence number already exists for this drive.");
        }

        RecruitmentRound round = new RecruitmentRound(
                driveId, name.trim(), sequenceNumber, scheduledAt, venue, instructions);

        return rounds.save(round);
    }

    @Transactional(readOnly = true)
    public List<RecruitmentRound> listByDrive(Long driveId) {
        return rounds.findByDriveIdOrderBySequenceNumberAsc(driveId);
    }

    public RecruitmentRound getById(Long id) {
        return rounds.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Recruitment round not found."));
    }
}