package com.aqar.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	List<RefreshTokenEntity> findAllByFamilyId(String familyId);
}