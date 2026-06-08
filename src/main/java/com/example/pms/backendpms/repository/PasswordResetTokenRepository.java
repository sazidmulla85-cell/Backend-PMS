package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.PasswordResetToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

  List<PasswordResetToken> findAllByUserAccountIdAndUsedAtIsNull(Long userAccountId);
}
