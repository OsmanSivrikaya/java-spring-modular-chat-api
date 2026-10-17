package com.example.modularchatapi.modules.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * Sisteme kayıtlı kullanıcı.
 * <p>
 * Spring Security'nin {@link UserDetails} arayüzünü uyguladığı için doğrudan
 * kimlik doğrulamada kullanılır. Giriş email ile yapılır, bu yüzden
 * {@link #getUsername()} email döndürür. Görünen kullanıcı adı için {@link #getDisplayName()}.
 */
// get edebilmek için
@Getter
// set edebilmek için
@Setter
// .net deki gibi obje tanılama yok o yüzden
@Builder
// boş ctor oluşturur veri tabanından okurken reflection ile önce boş nesne üretmek zorundadır
@NoArgsConstructor
// tüm alanların dolduğu ctor oluşturur builder ile bağlantılı
@AllArgsConstructor
// class değil veri tabanı tablosu demek için
@Entity
@Table(name = "users")
public class User implements UserDetails {
    /** Kullanıcının id'si, veritabanı otomatik artırır. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Giriş için kullanılan email, benzersiz ve küçük harfle saklanır. */
    @Column(nullable = false, unique = true)
    private String email;
    /** BCrypt ile hashlenmiş şifre. Düz şifre asla saklanmaz. */
    @Column(nullable = false)
    private String password;
    /** Ekranda görünen kullanıcı adı. */
    @Column(nullable = false)
    private String username;
    /** Kullanıcının yetkisi (USER / ADMIN). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    // Spring Security UserDetails Metotları (.NET ClaimsIdentity karşılığı)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override
    public String getUsername() {
        return email; // Giriş yaparken email kullanıyoruz
    }
    // getUsername email döndürdüğü için asıl kullanıcı adını buradan alıyoruz
    /**
     * Ekranda gösterilecek kullanıcı adını döndürür.
     *
     * @return kayıt olurken girilen kullanıcı adı
     */
    public String getDisplayName() {
        return username;
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public boolean isEnabled() {
        return true;
    }
}