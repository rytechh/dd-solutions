package br.com.rytechh.greenshift.adapters.out.persistence.entity;

import br.com.rytechh.greenshift.core.domain.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workload_job")
public class WorkloadJobEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String comando;

    private String descricao;

    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    @Column(nullable = false)
    private Instant deadline;

    @Column(name = "consumo_estimado_kw", nullable = false)
    private double consumoEstimadoKw;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "agendado_para")
    private Instant agendadoPara;

    @Column(name = "intensidade_janela_gco2_kwh", nullable = false)
    private double intensidadeJanelaGco2Kwh;

    @Column(name = "intensidade_baseline_gco2_kwh", nullable = false)
    private double intensidadeBaselineGco2Kwh;

    protected WorkloadJobEntity() {
        // exigido pelo JPA
    }

    public WorkloadJobEntity(UUID id, String comando, String descricao, int duracaoMinutos,
                              Instant deadline, double consumoEstimadoKw, Instant criadoEm,
                              JobStatus status, Instant agendadoPara,
                              double intensidadeJanelaGco2Kwh, double intensidadeBaselineGco2Kwh) {
        this.id = id;
        this.comando = comando;
        this.descricao = descricao;
        this.duracaoMinutos = duracaoMinutos;
        this.deadline = deadline;
        this.consumoEstimadoKw = consumoEstimadoKw;
        this.criadoEm = criadoEm;
        this.status = status;
        this.agendadoPara = agendadoPara;
        this.intensidadeJanelaGco2Kwh = intensidadeJanelaGco2Kwh;
        this.intensidadeBaselineGco2Kwh = intensidadeBaselineGco2Kwh;
    }

    public UUID getId() {
        return id;
    }

    public String getComando() {
        return comando;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public double getConsumoEstimadoKw() {
        return consumoEstimadoKw;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getAgendadoPara() {
        return agendadoPara;
    }

    public double getIntensidadeJanelaGco2Kwh() {
        return intensidadeJanelaGco2Kwh;
    }

    public double getIntensidadeBaselineGco2Kwh() {
        return intensidadeBaselineGco2Kwh;
    }
}
