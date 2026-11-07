
import { apiRequest } from './client.js';

// Admin — recruitment rounds for a placement drive
export const listDriveRounds = (driveId) =>
  apiRequest(`/admin/drives/${driveId}/rounds`);

export const createDriveRound = (driveId, body) =>
  apiRequest(`/admin/drives/${driveId}/rounds`, {
    method: 'POST',
    body,
  });

// Admin — applications assigned to a round
export const listRoundParticipants = (roundId) =>
  apiRequest(`/admin/rounds/${roundId}/participants`);

export const assignApplicationToRound = (roundId, applicationId) =>
  apiRequest(`/admin/rounds/${roundId}/participants`, {
    method: 'POST',
    body: { applicationId },
  });

export const updateRoundParticipantOutcome = (
  roundId,
  participantId,
  outcome,
  feedback
) =>
  apiRequest(`/admin/rounds/${roundId}/participants/${participantId}/outcome`, {
    method: 'PUT',
    body: { outcome, feedback },
  });
