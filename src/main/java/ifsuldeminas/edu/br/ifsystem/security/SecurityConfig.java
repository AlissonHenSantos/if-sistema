package ifsuldeminas.edu.br.ifsystem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<JwtFilter> jwtFilterRegistration(
            JwtFilter filter) {
        var registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter)
            throws Exception {
        return http.addFilterBefore(
                        jwtFilter,
                        org.springframework.security.web.authentication
                                .UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                        (request, response, exception) -> response.sendError(401)))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/rooms/**",
                                                "/api/reservations/**",
                                                "/api/keys/**")
                                        .authenticated()
                                        .anyRequest()
                                        .permitAll())
                .build();
    }
}
