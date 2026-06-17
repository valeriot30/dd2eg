package com.dd2eg.backend.beans;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Sblocco Auth e riga di Errore (fondamentale per vedere i veri errori HTTP e non finti 403)
                        .requestMatchers("/api/auth/**", "/error").permitAll()

                        // Regole specifiche di ruolo (vanno in alto)
                        .requestMatchers("/api/recommendations/financing").hasAuthority("ENTERPRISE")
                        .requestMatchers("/api/tasks/*/fund").hasAuthority("ENTERPRISE")
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // IL FIX: Dichiariamo sia la radice esatta, sia i path successivi!
                        .requestMatchers("/api/projects", "/api/projects/**").permitAll()
                        .requestMatchers("/api/tasks", "/api/tasks/**").permitAll()
                        .requestMatchers("/api/users", "/api/users/**").permitAll()
                        .requestMatchers("/api/recommendations", "/api/recommendations/**").permitAll()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}