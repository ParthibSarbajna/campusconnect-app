
package com.campusconnect.round;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/rounds/{roundId}/participants")
@PreAuthorize("hasRole('PLACEMENT_ADMIN')")
public class AdminRoundParticipantController {

    private final RecruitmentRoundParticipantService participants;

    public AdminRoundParticipantController(
            RecruitmentRoundParticipantService participants) {
        this.participants = participants;
    }

    public record AssignRequest(
            @NotNull Long applicationId
    ) {}

    public record OutcomeRequest(
            @NotNull RoundParticipantOutcome outcome,
            String feedback
    ) {}

    @GetMapping
    public java.util.List<RecruitmentRoundParticipant> list(
            @PathVariable Long roundId) {
        return participants.listByRound(roundId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecruitmentRoundParticipant assign(
            @PathVariable Long roundId,
            @Valid @RequestBody AssignRequest request) {
        return participants.assign(roundId, request.applicationId());
    }

    @PutMapping("/{participantId}/outcome")
    public RecruitmentRoundParticipant updateOutcome(
            @PathVariable Long roundId,
            @PathVariable Long participantId,
            @Valid @RequestBody OutcomeRequest request) {
        return participants.updateOutcome(
                roundId, participantId, request.outcome(), request.feedback());
    }
}
