package com.example.modularchatapi.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DataAnnotations ile yazılmış request
public record RegisterRequest(
        @NotBlank(message = "Kullanıcı adı boş olamaz")
        @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter olmalıdır")
        String username,
        @NotBlank(message = "Email boş olamaz")
        @Email(message = "Geçerli bir email adresi giriniz")
        String email,
        @NotBlank(message = "Şifre boş olamaz")
        @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
        String password
) {}