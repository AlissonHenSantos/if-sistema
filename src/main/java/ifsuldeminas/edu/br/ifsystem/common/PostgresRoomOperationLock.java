package ifsuldeminas.edu.br.ifsystem.common;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("!demo & !dev")
public class PostgresRoomOperationLock implements RoomOperationLock {
    private final JdbcTemplate jdbc;

    public PostgresRoomOperationLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void acquire(String roomId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Room lock requires a transaction");
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> {}, roomId);
    }
}
