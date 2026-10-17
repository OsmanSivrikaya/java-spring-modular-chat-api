package com.example.modularchatapi.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

// .NET'teki global exception middleware karşılığı
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email veya şifre hatalı");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        // Map = C#'taki Dictionary. Burada alan adı -> hata mesajı tutuyorum ("email" -> "Geçerli bir email giriniz").
        // Map interface, LinkedHashMap onun bir implementasyonu (IDictionary / Dictionary gibi).
        // HashMap yerine LinkedHashMap seçtim çünkü eklediğim sırayı koruyor, JSON'da alanlar düzgün sırayla çıksın.
        Map<String, String> errors = new LinkedHashMap<>();
        // getBindingResult(): @Valid kontrolünün sonucu (.NET'teki ModelState)
        // getFieldErrors(): hatalı alanların listesi
        // putIfAbsent: anahtar yoksa ekle. Bir alanda iki kural birden patlarsa (boş + geçersiz email gibi)
        // sadece ilk mesajı tutuyorum. C#'taki TryAdd ile aynı.
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        // 400 dönen standart hata cevabı (.NET'teki ProblemDetails)
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Doğrulama hatası");
        // Standartta olmayan ekstra alan ekliyorum, JSON'da "errors": { ... } olarak çıkıyor
        pd.setProperty("errors", errors);
        return pd;
    }
}
