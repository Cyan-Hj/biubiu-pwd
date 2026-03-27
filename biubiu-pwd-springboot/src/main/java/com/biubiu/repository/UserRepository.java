package com.biubiu.repository;

import com.biubiu.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE u.role = 'player' " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:level IS NULL OR u.level = :level) " +
           "AND (:search IS NULL OR u.nickname LIKE %:search% OR u.phone LIKE %:search%)")
    Page<User> findPlayers(@Param("status") User.Status status,
                          @Param("level") String level,
                          @Param("search") String search,
                          Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'player' AND u.status = 'active'")
    List<User> findActivePlayers();

    long countByStatus(User.Status status);

    long countByRole(User.Role role);

    long countByRoleAndStatus(User.Role role, User.Status status);
}
