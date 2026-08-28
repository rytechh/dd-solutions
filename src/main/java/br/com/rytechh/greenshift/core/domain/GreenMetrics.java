package br.com.rytechh.greenshift.core.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoria ecológica: quanto carbono foi evitado ao executar
 * um job na janela otimizada em vez de imediatamente.
 */
public class GreenMetrics {

    private final UUID id;
    private final UUID jobId;
    private final Instant executedAt;
    private final double carbonSavedGrams;

    public GreenMetrics(UUID id, UUID jobId, Instant executedAt, double carbonSavedGrams) {
        this.id = id;
        this.jobId = jobId;
        this.executedAt = executedAt;
        this.carbonSavedGrams = carbonSavedGrams;
    }

    public static GreenMetrics novo(UUID jobId, Instant executedAt, double carbonSavedGrams) {
        return new GreenMetrics(UUID.randomUUID(), jobId, executedAt, Math.max(0, carbonSavedGrams));
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public double getCarbonSavedGrams() {
        return carbonSavedGrams;
    }
}
