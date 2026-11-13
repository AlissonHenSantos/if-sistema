package ifsuldeminas.edu.br.ifsystem.keycontrol.repository;

import ifsuldeminas.edu.br.ifsystem.keycontrol.domain.KeyMovement;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.time.Instant;
import java.util.*;

public interface KeyMovementRepository extends JpaRepository<KeyMovement, UUID> {
    boolean existsByRoomIdAndCheckinAtIsNull(String roomId);

    boolean existsByReservationIdAndCheckinAtIsNull(UUID reservationId);

    List<KeyMovement> findByCheckinAtIsNull();

    @Query(
            """
            select m from KeyMovement m where (:roomId is null or m.roomId = :roomId)
            and (:responsibleUserId is null or m.responsibleUser.id = :responsibleUserId)
            and m.checkoutAt < :end and (m.checkinAt is null or m.checkinAt > :start)
            """)
    Page<KeyMovement> search(
            String roomId, UUID responsibleUserId, Instant start, Instant end, Pageable pageable);
}
