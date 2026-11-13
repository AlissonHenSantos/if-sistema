package ifsuldeminas.edu.br.ifsystem.common;

import org.springframework.context.annotation.*;

import java.time.Clock;

@Configuration
public class TimeConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
