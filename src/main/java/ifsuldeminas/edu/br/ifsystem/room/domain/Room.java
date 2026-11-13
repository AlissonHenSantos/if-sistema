package ifsuldeminas.edu.br.ifsystem.room.domain;

public record Room(
        String id,
        String name,
        String building,
        Integer capacity,
        String type,
        String description) {}
