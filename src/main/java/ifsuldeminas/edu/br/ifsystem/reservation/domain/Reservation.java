package ifsuldeminas.edu.br.ifsystem.reservation.domain;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "reservations",
        indexes = {
            @Index(
                    name = "ix_reservation_room_period",
                    columnList = "roomId,status,startTime,endTime"),
            @Index(name = "ix_reservation_requester", columnList = "requester_id")
        })
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String roomId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Usuario requester;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario reviewedBy;

    private Instant reviewedAt;

    @Column(length = 1000)
    private String rejectionReason;

    @Version private long version;

    protected Reservation() {}

    public Reservation(String roomId, Usuario requester, Instant start, Instant end) {
        this.roomId = roomId;
        this.requester = requester;
        this.startTime = start;
        this.endTime = end;
        status = ReservationStatus.PENDING;
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    public void review(ReservationStatus decision, Usuario operator, String reason) {
        status = decision;
        reviewedBy = operator;
        reviewedAt = Instant.now();
        updatedAt = reviewedAt;
        rejectionReason = reason;
    }

    public void cancel() {
        status = ReservationStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public Usuario getRequester() {
        return requester;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Usuario getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
