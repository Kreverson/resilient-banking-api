package com.resilientbanking.infra.persistence.repository;

import com.resilientbanking.infra.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select t from TransactionEntity t
            where t.sourceAccountId = :accountId or t.targetAccountId = :accountId
            order by t.createdAt desc""")
    List<TransactionEntity> findRecentByAccount(@Param("accountId") Long accountId, Limit limit);
}
