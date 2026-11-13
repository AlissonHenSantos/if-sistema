package ifsuldeminas.edu.br.ifsystem.common;

/** Serializes critical reservation and key operations until the transaction ends. */
public interface RoomOperationLock {
    void acquire(String roomId);
}
