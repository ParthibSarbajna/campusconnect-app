package com.campusconnect.round;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/drives/{driveId}/rounds")
@PreAuthorize("hasRole('PLACEMENT_ADMIN')")
public class AdminRecruitmentRoundController {

    private final RecruitmentRoundService rounds;

    public AdminRecruitmentRoundController(RecruitmentRoundService rounds) {
        this.rounds = rounds;
    }

    public record RoundRequest(
            @NotBlank String name,
            @NotNull @Positive Integer sequenceNumber,
            Instant scheduledAt,
            String venue,
            String instructions
    ) {}

    @GetMapping
    public List<RecruitmentRound> list(@PathVariable Long driveId) {
        return rounds.listByDrive(driveId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecruitmentRound create(
            @PathVariable Long driveId,
            @Valid @RequestBody RoundRequest request) {
        return rounds.create(
                driveId,
                request.name(),
                request.sequenceNumber(),
                request.scheduledAt(),
                request.venue(),
                request.instructions()
        );
    }
}