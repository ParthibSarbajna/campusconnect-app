
package com.campusconnect.round;

import com.campusconnect.application.Application;
import com.campusconnect.application.ApplicationRepository;
import com.campusconnect.common.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecruitmentRoundParticipantService {

    private final RecruitmentRoundRepository rounds;
    private final RecruitmentRoundParticipantRepository participants;
    private final ApplicationRepository applications;

    public RecruitmentRoundParticipantService(
            RecruitmentRoundRepository rounds,
            RecruitmentRoundParticipantRepository participants,
            ApplicationRepository applications) {
        this.rounds = rounds;
        this.participants = participants;
        this.applications = applications;
    }

    public RecruitmentRoundParticipant assign(Long roundId, Long applicationId) {
        RecruitmentRound round = rounds.findById(roundId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Recruitment round not found."));

        Application application = applications.findByIdAndDriveId(
                        applicationId, round.getDriveId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Application does not belong to this placement drive."));

        if (participants.findByRoundIdAndApplicationId(roundId, applicationId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Application is already assigned to this round.");
        }

        return participants.save(
                new RecruitmentRoundParticipant(roundId, application.getId()));
    }

    @Transactional(readOnly = true)
    public List<RecruitmentRoundParticipant> listByRound(Long roundId) {
        if (!rounds.existsById(roundId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Recruitment round not found.");
        }

        return participants.findByRoundIdOrderByIdAsc(roundId);
    }

    public RecruitmentRoundParticipant updateOutcome(
            Long roundId,
            Long participantId,
            RoundParticipantOutcome outcome,
            String feedback) {

        RecruitmentRoundParticipant participant = participants.findById(participantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Round participant not found."));

        if (!participant.getRoundId().equals(roundId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Participant does not belong to this round.");
        }

        participant.updateOutcome(outcome, feedback);
        return participants.save(participant);
    }
}
