package com.example.modularchatapi.modules.auth.domain;

import lombok.Getter;

// @Getter (Lombok kütüphanesi), derleme anında (sen kodu yazarken) senin yerine arka planda görünmez bir şekilde public int getValue() metodunu oluşturur!
/** Kullanıcı yetkisi. Spring Security'de "ROLE_USER" / "ROLE_ADMIN" olarak görünür. */
@Getter
public enum Role {
    /** Normal kullanıcı. */
    USER(1),
    /** Yönetici. */
    ADMIN(2);

    private final int value;
    // Enum Constructor'ı
    Role(int value) {
        this.value = value;
    }
}
