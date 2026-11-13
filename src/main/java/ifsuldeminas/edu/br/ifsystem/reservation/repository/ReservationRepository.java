package ifsuldeminas.edu.br.ifsystem.reservation.repository;

import ifsuldeminas.edu.br.ifsystem.reservation.domain.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

import java.time.Instant;
import java.util.*;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    @Query(
            "select r from Reservation r where r.roomId = :roomId and r.status = :status and"
                + " r.startTime < :end and r.endTime > :start")
    List<Reservation> findOverlapping(
            String roomId, ReservationStatus status, Instant start, Instant end);

    @Query(
            """
            select r from Reservation r where (:roomId is null or r.roomId = :roomId)
            and (:requesterId is null or r.requester.id = :requesterId)
            and (:status is null or r.status = :status)
            and r.startTime < :end and r.endTime > :start
            """)
    Page<Reservation> search(
            String roomId,
            UUID requesterId,
            ReservationStatus status,
            Instant start,
            Instant end,
            Pageable pageable);
}
