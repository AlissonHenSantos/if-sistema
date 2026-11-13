package ifsuldeminas.edu.br.ifsystem.room.provider;

import ifsuldeminas.edu.br.ifsystem.room.domain.Room;

import java.util.List;
import java.util.Optional;

public interface RoomDataProvider {
    List<Room> findAll();

    default Optional<Room> findById(String id) {
        return findAll().stream().filter(room -> room.id().equals(id)).findFirst();
    }
}
