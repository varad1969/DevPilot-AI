package devPilot.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import devPilot.backend.security.GithubOAuth2UserService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GithubOAuth2UserService gitHubOAuth2UserService;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler oauth2SuccessHandler,
            AuthenticationFailureHandler oauth2FailureHandler) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/actuator/**",
                                "/api/debug/**",
                                "/api/auth/login-url",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(gitHubOAuth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("DEVPILOT_SESSION"));

        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(
            @Value("${app.frontend-url}") String frontendUrl) {
        String cleanUrl = frontendUrl.replaceAll("/+$", "");
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl(cleanUrl + "/auth/callback");
        return handler;
    }

    @Bean
    AuthenticationFailureHandler oauth2FailureHandler(
            @Value("${app.frontend-url}") String frontendUrl) {
        String cleanUrl = frontendUrl.replaceAll("/+$", "");
        return (request, response, exception) -> {
            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                    .error("OAuth2 login failed: {}", exception.getMessage(), exception);
            String errorMsg = exception.getMessage() != null ? exception.getMessage() : "oauth_failed";
            response.sendRedirect(cleanUrl + "/login?error=" + java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8));
        };
    }
}
