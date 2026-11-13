package ifsuldeminas.edu.br.ifsystem;

import static org.junit.jupiter.api.Assertions.*;

import ifsuldeminas.edu.br.ifsystem.common.DemoRoomOperationLock;
import ifsuldeminas.edu.br.ifsystem.common.RoomOperationLock;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@SpringBootTest
@ActiveProfiles("dev")
class DevProfileTest {
    @Autowired DataSource dataSource;
    @Autowired RoomOperationLock lock;
    @Autowired UsuarioRepository users;
    @Autowired Environment environment;

    @Test
    @Transactional
    void developmentUsesJpaWithH2AndConfiguresTheRealSpreadsheet() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:ifsystem-dev"));
        }
        assertInstanceOf(DemoRoomOperationLock.class, lock);
        String location = environment.getRequiredProperty("app.rooms.location");
        assertTrue(
                location.startsWith(
                        "https://docs.google.com/spreadsheets/d/1KKREZRFIJuv5r7Oj8v7Eak54i_xZWs8bt-trL9PEY6Y/"));
        var user =
                users.saveAndFlush(
                        new Usuario(
                                "Development test",
                                "90000000002",
                                "dev-profile@ifsuldeminas.edu.br",
                                "dev-profile",
                                "unused-password",
                                null,
                                null,
                                null,
                                Perfil.SERVIDOR));
        assertTrue(users.existsById(user.getId()));

    }
}
