package com.example.modularchatapi.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Program.cs'te AddAuthentication + AddAuthorization + UseAuthentication ne yapıyorsa burada o var.
// @Configuration: bu sınıf ayar sınıfı, içindeki @Bean'leri DI'ye kaydet demek.
@Configuration
// Her HTTP isteğini güvenlik filtrelerinden geçiren yapı .netdeki UseAuthentication ve UseAuthorization gibi
// aslında bunu yazmaya gerek yok sprint otomatik basıyormuş ama güvenlik burdan yönetiliyor demek içinmiş
@EnableWebSecurity
// ctoru otomatik doldurmak için yani .netdeki private ctor mantığı
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Bean = Spring'in yönettiği nesne. DI container'ında duran her şeye bean deniyor.
    // @Bean: "bu metodun döndürdüğü nesneyi container'a kaydet" demek.
    // .NET'teki builder.Services.AddSingleton<SecurityFilterChain>(sp => ...) gibi.
    // Kendi sınıflarıma @Service/@Component yazıp kaydettiriyorum. @Bean'i ise
    // kodunu değiştiremediğim hazır sınıflar için (BCryptPasswordEncoder gibi) kullanıyorum.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Cookie kullanmıyorum, JWT ile gidiyorum, o yüzden CSRF'e gerek yok
                .csrf(AbstractHttpConfigurer::disable)
                // Session tutma, her istek kendi token'ıyla gelsin (stateless)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // .NET'te controller'lara [Authorize] / [AllowAnonymous] yazıyordum.
                // Spring'de hepsini burada tek yerden söylüyorum. Sıra önemli, ilk eşleşen kazanır.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Socket handshake açık, token'ı STOMP CONNECT'te kontrol ediyorum (JwtStompInterceptor)
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/", "/index.html", "/error").permitAll()
                        .anyRequest().authenticated())
                // Giriş yapılmamışsa login sayfasına yönlendirme yapmasın, direkt 401 dönsün
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Middleware sırası burada belli oluyor: benim JWT filter'ım, Spring'in
                // kullanıcı adı/şifre filter'ından ÖNCE çalışsın. Böylece istek oraya
                // vardığında kullanıcı zaten tanınmış oluyor.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Şifreleri BCrypt ile hashliyorum. .NET'teki PasswordHasher<T> gibi.
    // Bean olduğu için AuthService'te ctor'a PasswordEncoder yazınca bu nesne geliyor.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Login'de email + şifreyi kontrol eden yapı. AuthService'te kullanıyorum.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
