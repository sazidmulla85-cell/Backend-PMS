package com.example.pms.backendpms.repository;

import com.example.pms.backendpms.model.UserAccount;
import com.example.pms.backendpms.model.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByEmailIgnoreCase(String email);

  Optional<UserAccount> findByPhone(String phone);

  List<UserAccount> findAllByEmailIgnoreCase(String email);

  List<UserAccount> findByRole(UserRole role);

  long countByRole(UserRole role);

  List<UserAccount> findAllByOrderByLastLoginAtDesc();
}
