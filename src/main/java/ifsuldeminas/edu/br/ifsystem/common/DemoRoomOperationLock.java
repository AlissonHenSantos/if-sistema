package ifsuldeminas.edu.br.ifsystem.common;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Local H2 profiles only: a shared database row serializes all rooms within transactions. */
@Component
@Profile({"demo", "dev"})
public class DemoRoomOperationLock implements RoomOperationLock {
    private final JdbcTemplate jdbc;

    public DemoRoomOperationLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void acquire(String roomId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Room lock requires a transaction");
        jdbc.queryForObject(
                "select id from demo_operation_lock where id = 1 for update", Integer.class);
    }
}
