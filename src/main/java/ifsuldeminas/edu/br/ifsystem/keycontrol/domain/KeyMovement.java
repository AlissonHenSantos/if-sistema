package ifsuldeminas.edu.br.ifsystem.keycontrol.domain;

import ifsuldeminas.edu.br.ifsystem.reservation.domain.Reservation;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "key_movements",
        indexes = {
            @Index(name = "ix_key_room_active", columnList = "roomId,checkinAt"),
            @Index(
                    name = "ix_key_responsible_period",
                    columnList = "responsible_user_id,checkoutAt")
        })
public class KeyMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String roomId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Reservation reservation;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Usuario responsibleUser;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Usuario processedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario returnedByUser;

    @Column(nullable = false, updatable = false)
    private Instant checkoutAt;

    private Instant checkinAt;
    @Version private long version;

    protected KeyMovement() {}

    public KeyMovement(Reservation reservation, Usuario operator, Instant now) {
        this.reservation = reservation;
        this.roomId = reservation.getRoomId();
        this.responsibleUser = reservation.getRequester();
        this.processedByUser = operator;
        this.checkoutAt = now;
    }

    public void checkin(Usuario operator, Instant now) {
        checkinAt = now;
        returnedByUser = operator;
    }

    public UUID getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Usuario getResponsibleUser() {
        return responsibleUser;
    }

    public Usuario getProcessedByUser() {
        return processedByUser;
    }

    public Usuario getReturnedByUser() {
        return returnedByUser;
    }

    public Instant getCheckoutAt() {
        return checkoutAt;
    }

    public Instant getCheckinAt() {
        return checkinAt;
    }
}
