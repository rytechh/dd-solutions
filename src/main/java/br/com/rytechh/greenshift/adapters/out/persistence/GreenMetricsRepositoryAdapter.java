package br.com.rytechh.greenshift.adapters.out.persistence;

import br.com.rytechh.greenshift.adapters.out.persistence.entity.GreenMetricsEntity;
import br.com.rytechh.greenshift.application.ports.out.GreenMetricsRepositoryPort;
import br.com.rytechh.greenshift.core.domain.GreenMetrics;

import java.util.List;

public class GreenMetricsRepositoryAdapter implements GreenMetricsRepositoryPort {

    private final GreenMetricsJpaRepository jpaRepository;

    public GreenMetricsRepositoryAdapter(GreenMetricsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public GreenMetrics salvar(GreenMetrics metrics) {
        jpaRepository.save(new GreenMetricsEntity(metrics.getId(), metrics.getJobId(),
                metrics.getExecutedAt(), metrics.getCarbonSavedGrams()));
        return metrics;
    }

    @Override
    public List<GreenMetrics> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(e -> new GreenMetrics(e.getId(), e.getJobId(), e.getExecutedAt(), e.getCarbonSavedGrams()))
                .toList();
    }
}
