package ifsuldeminas.edu.br.ifsystem.reservation.api;

import ifsuldeminas.edu.br.ifsystem.reservation.domain.*;

import java.time.Instant;
import java.util.UUID;

public final class ReservationDtos {
    private ReservationDtos() {}

    public record CreateReservation(String roomId, Instant startTime, Instant endTime) {}

    public record RejectReservation(String reason) {}

    public record ReservationResponse(
            UUID id,
            String roomId,
            UUID requesterId,
            Instant startTime,
            Instant endTime,
            ReservationStatus status,
            Instant createdAt,
            Instant updatedAt,
            UUID reviewedBy,
            Instant reviewedAt,
            String rejectionReason) {
        public static ReservationResponse from(Reservation r) {
            return new ReservationResponse(
                    r.getId(),
                    r.getRoomId(),
                    r.getRequester().getId(),
                    r.getStartTime(),
                    r.getEndTime(),
                    r.getStatus(),
                    r.getCreatedAt(),
                    r.getUpdatedAt(),
                    r.getReviewedBy() == null ? null : r.getReviewedBy().getId(),
                    r.getReviewedAt(),
                    r.getRejectionReason());
        }
    }
}
