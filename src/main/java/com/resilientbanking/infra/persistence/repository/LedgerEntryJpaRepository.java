package com.resilientbanking.infra.persistence.repository;

import com.resilientbanking.infra.persistence.entity.LedgerEntryEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, Long> {

    List<LedgerEntryEntity> findByAccountIdOrderByIdDesc(Long accountId, Limit limit);
}
