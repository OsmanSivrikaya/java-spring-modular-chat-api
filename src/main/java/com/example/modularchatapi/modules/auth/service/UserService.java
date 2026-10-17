package com.example.modularchatapi.modules.auth.service;

import com.example.modularchatapi.modules.auth.api.UserQueryApi;
import com.example.modularchatapi.modules.auth.domain.User;
import com.example.modularchatapi.modules.auth.dto.internal.UserSummaryDto;
import com.example.modularchatapi.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService, UserQueryApi {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));
    }

    @Override
    public Optional<UserSummaryDto> findById(Long id) {
        return userRepository.findById(id).map(UserService::toSummary);
    }

    @Override
    public Optional<UserSummaryDto> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserService::toSummary);
    }

    @Override
    public List<UserSummaryDto> findAllByIds(Collection<Long> ids) {
        return userRepository.findAllById(ids).stream().map(UserService::toSummary).toList();
    }

    @Override
    public List<UserSummaryDto> search(String query) {
        return userRepository.findTop20ByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .stream().map(UserService::toSummary).toList();
    }

    public static UserSummaryDto toSummary(User u) {
        return new UserSummaryDto(u.getId(), u.getDisplayName(), u.getEmail());
    }
}
