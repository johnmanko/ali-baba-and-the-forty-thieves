package com.johnmanko.portfolio.alibabassecret.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * Annotation: @EnableMethodSecurity - Securing Individual Methods
 * Purpose: Enables Spring Security's method-level security
 * Scope: Method-level security (@PreAuthorize, @Secured, @RolesAllowed)
 * Common Use Cases: Securing methods, pre/post annotations, custom annotations, role-based access control at service/controller level
 * 1. Enables method-level security annotations like: @PreAuthorize, @Secured, @RolesAllowed
 * 2. Ensures security inside controllers and service classes, not just at the HTTP level.
 * 3. Protecting business logic inside services.
 * 4. Controlling access to controller methods based on roles or permissions.
 *
 * Annotation: @EnableWebSecurity  - Securing Web Requests (Filters)
 * Purpose: Configures HTTP security
 * Scope: Web requests (filter-based security). Used in combination with Spring Security’s HttpSecurity DSL.
 * Common Use Cases: Securing URLs, CSRF, CORS, session management.
 * 1. Configures HTTP request security.
 * 2. Defines authentication and authorization rules for URL paths.
 * 3. Used in combination with Spring Security’s HttpSecurity DSL.
 * 4. Defining URL-based security rules.
 * 5. Managing authentication mechanisms (JWT, OAuth2, Basic Auth, etc.).
 * 6. Configuring CORS, CSRF, and session management.
 *
 * References:
 * https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html
 * https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
 * https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-securityfilterchain
 * https://docs.spring.io/spring-security/reference/servlet/configuration/java.html#jc-httpsecurity
 * https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-architecture
 * https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
 */
@Configuration
@EnableMethodSecurity
//@EnableWebSecurity - Not needed for @Bean SecurityFilterChain in Spring Security 6.0+
public class SecurityConfig {

    @Value("${app.config.server.auth.disable-csrf}")
    private boolean disableCsrf;
    @Value("${app.config.server.auth.auth0.custom-jwt-namespace}")
    private String customJwtNamespace;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/public/**").permitAll()
                    .anyRequest().authenticated()
            )
            .cors(Customizer.withDefaults())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        if (disableCsrf) {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("permissions");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = extractRoles(jwt);
            authorities.addAll(grantedAuthoritiesConverter.convert(jwt));
            return authorities;
        });
        return jwtAuthenticationConverter;
    }

    private List<GrantedAuthority> extractRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(customJwtNamespace);
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // Prefix with "ROLE_"
                .collect(Collectors.toList());
    }

}