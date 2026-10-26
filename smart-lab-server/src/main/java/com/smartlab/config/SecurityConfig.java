package com.smartlab.config;

import com.smartlab.common.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtils jwtUtils;

    public SecurityConfig(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/upload/**", "/doc.html", "/webjars/**", "/v3/api-docs/**", "/uploads/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/log/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/borrow/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/repair/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/device/**").permitAll()
                .requestMatchers("/api/device/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers("/api/borrow/apply").authenticated()
                .requestMatchers("/api/borrow/batch").authenticated()
                .requestMatchers("/api/borrow/return/**").authenticated()
                .requestMatchers("/api/borrow/cancel/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/borrow/**").authenticated()
                .requestMatchers("/api/borrow/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers("/api/repair/report").authenticated()
                .requestMatchers("/api/repair/assign/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers("/api/repair/complete/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/repair/**").authenticated()
                .requestMatchers("/api/repair/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/lab/**").permitAll()
                .requestMatchers("/api/lab/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers("/api/lab-reservation/apply").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/lab-reservation/**").authenticated()
                .requestMatchers("/api/lab-reservation/approve/**").hasAnyAuthority("ADMIN", "LAB_ADMIN")
                .requestMatchers("/api/recyclebin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/announcement/**").authenticated()
                .requestMatchers("/api/statistics/**").hasAnyAuthority("ADMIN", "LAB_ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/export/device").hasAnyAuthority("ADMIN", "LAB_ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/export/**").hasAnyAuthority("ADMIN", "LAB_ADMIN", "TEACHER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static class JwtAuthenticationFilter extends OncePerRequestFilter {
        private final JwtUtils jwtUtils;

        public JwtAuthenticationFilter(JwtUtils jwtUtils) {
            this.jwtUtils = jwtUtils;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!jwtUtils.isTokenExpired(token)) {
                    var claims = jwtUtils.parseToken(token);
                    String role = claims.get("role", String.class);
                    Long userId = claims.get("userId", Long.class);
                    String username = claims.get("username", String.class);

                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
                    var authentication = new UsernamePasswordAuthenticationToken(
                            new UserPrincipal(userId, username, role), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        }
    }

    public record UserPrincipal(Long userId, String username, String role) {}
}