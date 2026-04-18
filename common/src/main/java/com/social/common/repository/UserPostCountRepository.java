package com.social.common.repository;

import com.social.common.entity.UserPostCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPostCountRepository extends JpaRepository<UserPostCount, Long> {
}
