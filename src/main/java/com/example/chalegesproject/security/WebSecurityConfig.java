package com.example.chalegesproject.security;

import com.example.chalegesproject.security.jwt.AuthEntryPointJwt;
import com.example.chalegesproject.security.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
//הגדרות אבטחה
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    @Qualifier("customUserDetailsService")
    CustomUserDetailsService userDetailsService;
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;
    public WebSecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception{
    return authConfig.getAuthenticationManager();}

    @Bean
    public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();}


        @Bean
        public SecurityFilterChain filterChain (HttpSecurity http) throws Exception {
            //משבית את הגנת CSRF על ידי הפעלת שיטת `csrf()` והשבתתה
            http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(request -> {
                        CorsConfiguration corsConfiguration = new CorsConfiguration();
                        corsConfiguration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:4200","http://localhost:65278"));
                        corsConfiguration.setAllowedMethods(List.of("*"));
                        corsConfiguration.setAllowedHeaders(List.of("*"));
                        corsConfiguration.setAllowCredentials(true);
                        return corsConfiguration;
                    }))

                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth ->
                                    auth.requestMatchers("/h2-console/**").permitAll()
                                            .requestMatchers("/api/users/sign**").permitAll()
                                            .requestMatchers("/api/challenges/**").permitAll()
                                            .requestMatchers("/api/users/chat**").permitAll()


                                            .requestMatchers("/error").permitAll()


//                  .requestMatchers("/api/user/signIn").permitAll()
                                            .anyRequest().authenticated()
                    );

            // fix H2 database console: Refused to display ' in a frame because it set 'X-Frame-Options' to 'deny'
            http.headers(headers -> headers.frameOptions(frameOption -> frameOption.sameOrigin()));
            http.authenticationProvider(authenticationProvider());


            http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);



            return http.build();
        }
    }
