package com.reelshop.security;

import com.reelshop.entity.Seller;
import com.reelshop.repository.SellerRepository;
import com.reelshop.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // Comma-separated list. Local default below; set CORS_ORIGINS in production
    // e.g. CORS_ORIGINS=https://nxtgenmart.in,https://www.nxtgenmart.in
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    // CORS is wired INTO Spring Security (via .cors(...) below) so that browser
    // preflight (OPTIONS) requests are answered before authentication is checked.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(allowedOrigins.stream().map(String::trim).toList());
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Missing/expired token => 401 (the frontend redirects to sign-in on 401).
            // Wrong role (e.g. buyer calling a seller endpoint) => 403.
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/seller/check-slug").permitAll()
                .requestMatchers(HttpMethod.GET, "/s/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/p/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/track/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Role rules
                .requestMatchers("/seller/**", "/products/**", "/qr/**").hasRole("SELLER")
                .requestMatchers("/buyer/**", "/checkout/**", "/orders/**").hasRole("BUYER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Slf4j
    @Component
    @RequiredArgsConstructor
    public static class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtUtil          jwtUtil;
        private final SellerRepository sellerRepository;
        // Step 1B adds: private final BuyerRepository buyerRepository;

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain)
                throws ServletException, IOException {

            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) {
                    Long   userId = jwtUtil.extractUserId(token);
                    String role   = jwtUtil.extractRole(token);

                    if (JwtUtil.ROLE_SELLER.equals(role)) {
                        // Suspended / deleted sellers can no longer use their old token
                        sellerRepository.findById(userId)
                            .filter(s -> s.getStatus() == Seller.SellerStatus.ACTIVE)
                            .ifPresent(seller -> authenticate(seller, "ROLE_SELLER"));
                    }
                    // Step 1B adds the BUYER branch here
                }
            }
            chain.doFilter(request, response);
        }

        private void authenticate(Object principal, String authority) {
            var auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority(authority)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }
}
