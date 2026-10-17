package com.example.modularchatapi.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// .NET'teki middleware burada filter oluyor. AddJwtBearer'ın yaptığı işi ben elle yazdım.
// OncePerRequestFilter: "bir istekte sadece bir kere çalış" demek. Bir şeyden önce çalışmak
// anlamına gelmiyor. Spring bazen aynı isteği içeride forward/error'a yönlendirince filter
// tekrar tetiklenebiliyor, bu sınıf bunun önüne geçiyor. Neyden önce çalışacağını
// SecurityConfig'teki addFilterBefore belirliyor.
@Component
// Lombok: final olan alanlar için constructor'ı kendisi yazıyor (constructor injection).
// .NET'te ctor'a parametre yazıp private readonly alana atamanın kısa yolu.
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // final = C#'taki readonly. Bir kere atanıyor (ctor'da), sonra değiştirilemiyor.
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // Middleware'deki InvokeAsync(HttpContext context, RequestDelegate next) metodunun karşılığı
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        // Token yoksa hiçbir şey yapmadan sıradakine bırakıyorum.
        // Endpoint korumalıysa zaten security 401 döner.
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response); // = await next(context)
            return;
        }
        String token = header.substring(7); // "Bearer " kısmını atıyorum
        try {
            String email = jwtService.extractSubject(token);
            // Daha önce bu istekte kimlik set edilmediyse devam
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(email);
                if (jwtService.isTokenValid(token, user)) {
                    // claim benzeri nesne oluşturuyoruz
                    var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    // isteği kim attı istek bilgisi auth. details içine basıyoruz
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // .NET'te HttpContext.User'ı doldurmak gibi. Controller'da Principal buradan geliyor.
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (JwtException | UsernameNotFoundException ignored) {
            // Token bozuksa ya da süresi geçtiyse patlatmıyorum, kimliksiz devam ediyor.
            // Korumalı endpoint'e gelirse 401 alır.
        }
        chain.doFilter(request, response);
    }
}
