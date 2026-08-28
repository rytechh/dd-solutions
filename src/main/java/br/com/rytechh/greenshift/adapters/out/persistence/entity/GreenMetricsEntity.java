package br.com.rytechh.greenshift.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "green_metrics")
public class GreenMetricsEntity {

    @Id
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "carbon_saved_grams", nullable = false)
    private double carbonSavedGrams;

    protected GreenMetricsEntity() {
        // exigido pelo JPA
    }

    public GreenMetricsEntity(UUID id, UUID jobId, Instant executedAt, double carbonSavedGrams) {
        this.id = id;
        this.jobId = jobId;
        this.executedAt = executedAt;
        this.carbonSavedGrams = carbonSavedGrams;
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
