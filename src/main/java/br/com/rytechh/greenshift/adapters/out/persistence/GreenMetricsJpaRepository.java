package br.com.rytechh.greenshift.adapters.out.persistence;

import br.com.rytechh.greenshift.adapters.out.persistence.entity.GreenMetricsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GreenMetricsJpaRepository extends JpaRepository<GreenMetricsEntity, UUID> {
}
