package com.example.modularchatapi.modules.auth.api;

import com.example.modularchatapi.modules.auth.dto.internal.UserSummaryDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Auth modülünün diğer modüllere açtığı tek kapı (modüller arası sözleşme).
// Chat modülü User entity'sine veya UserRepository'ye doğrudan erişmez.
public interface UserQueryApi {
    Optional<UserSummaryDto> findById(Long id);
    Optional<UserSummaryDto> findByEmail(String email);
    List<UserSummaryDto> findAllByIds(Collection<Long> ids);
    List<UserSummaryDto> search(String query);
}
