package com.ssafy.tiggle.repository.donation;

import com.ssafy.tiggle.entity.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {

    Optional<UserCharacter> findByUserId(Long userId);
}
