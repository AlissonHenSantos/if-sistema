package ifsuldeminas.edu.br.ifsystem.keycontrol.api;

import ifsuldeminas.edu.br.ifsystem.keycontrol.domain.KeyMovement;

import java.time.Instant;
import java.util.UUID;

public final class KeyDtos {
    private KeyDtos() {}

    public record Checkout(UUID reservationId) {}

    public record Checkin(UUID movementId) {}

    public record MovementResponse(
            UUID id,
            String roomId,
            UUID reservationId,
            UUID responsibleUserId,
            UUID processedByUserId,
            UUID returnedByUserId,
            Instant checkoutAt,
            Instant checkinAt,
            String status) {
        public static MovementResponse from(KeyMovement m) {
            return new MovementResponse(
                    m.getId(),
                    m.getRoomId(),
                    m.getReservation().getId(),
                    m.getResponsibleUser().getId(),
                    m.getProcessedByUser().getId(),
                    m.getReturnedByUser() == null ? null : m.getReturnedByUser().getId(),
                    m.getCheckoutAt(),
                    m.getCheckinAt(),
                    m.getCheckinAt() == null ? "CHECKED_OUT" : "RETURNED");
        }
    }

    public record KeyStatus(
            String roomId, String status, UUID responsibleUserId, Instant checkoutAt) {}
}
