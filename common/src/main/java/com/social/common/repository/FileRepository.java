package com.social.common.repository;

import com.social.common.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    @Query("SELECT f FROM File f WHERE f.postId = :postId ORDER BY f.sortOrder ASC")
    List<File> findByPostIdOrderBySortOrderAsc(@Param("postId") Long postId);
}
