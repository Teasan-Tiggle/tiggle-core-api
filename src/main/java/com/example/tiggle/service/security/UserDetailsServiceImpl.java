package com.example.tiggle.service.security;

import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final StudentRepository studentRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

        Users user = studentRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                null
        );
    }
}
