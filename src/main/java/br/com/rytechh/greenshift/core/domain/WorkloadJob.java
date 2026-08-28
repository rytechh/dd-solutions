package br.com.rytechh.greenshift.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade central do domínio: representa uma carga de trabalho não crítica
 * (backup, batch, etc.) aguardando a melhor janela de baixa emissão de carbono.
 */
public class WorkloadJob {

    private static final double CONSUMO_PADRAO_KW = 5.0;

    private final UUID id;
    private final String comando;
    private final String descricao;
    private final int duracaoMinutos;
    private final Instant deadline;
    private final double consumoEstimadoKw;
    private final Instant criadoEm;

    private JobStatus status;
    private Instant agendadoPara;

    /** Intensidade de carbono (gCO2/kWh) prevista para a janela escolhida. */
    private double intensidadeJanelaGco2Kwh;

    /** Intensidade de carbono (gCO2/kWh) que seria paga executando o job imediatamente. */
    private double intensidadeBaselineGco2Kwh;

    public WorkloadJob(UUID id, String comando, String descricao, int duracaoMinutos,
                        Instant deadline, double consumoEstimadoKw, Instant criadoEm,
                        JobStatus status, Instant agendadoPara,
                        double intensidadeJanelaGco2Kwh, double intensidadeBaselineGco2Kwh) {
        if (duracaoMinutos <= 0) {
            throw new IllegalArgumentException("duracaoMinutos deve ser maior que zero");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("deadline é obrigatório");
        }
        this.id = Objects.requireNonNull(id);
        this.comando = Objects.requireNonNull(comando);
        this.descricao = descricao;
        this.duracaoMinutos = duracaoMinutos;
        this.deadline = deadline;
        this.consumoEstimadoKw = consumoEstimadoKw <= 0 ? CONSUMO_PADRAO_KW : consumoEstimadoKw;
        this.criadoEm = Objects.requireNonNull(criadoEm);
        this.status = Objects.requireNonNull(status);
        this.agendadoPara = agendadoPara;
        this.intensidadeJanelaGco2Kwh = intensidadeJanelaGco2Kwh;
        this.intensidadeBaselineGco2Kwh = intensidadeBaselineGco2Kwh;
    }

    public static WorkloadJob novo(String comando, String descricao, int duracaoMinutos,
                                    Instant deadline, double consumoEstimadoKw) {
        return new WorkloadJob(UUID.randomUUID(), comando, descricao, duracaoMinutos,
                deadline, consumoEstimadoKw, Instant.now(), JobStatus.PENDENTE, null, 0, 0);
    }

    /**
     * Confirma o agendamento na janela de menor emissão encontrada pelo motor de otimização,
     * guardando as duas intensidades que sustentam o cálculo da economia (auditoria ESG).
     *
     * @param janelaOtima          início da janela escolhida
     * @param intensidadeJanela    gCO2/kWh médios da janela escolhida
     * @param intensidadeBaseline  gCO2/kWh que seriam pagos executando agora
     */
    public void agendarPara(Instant janelaOtima, double intensidadeJanela, double intensidadeBaseline) {
        if (janelaOtima.isAfter(deadline)) {
            throw new IllegalStateException("Janela calculada excede o deadline do job");
        }
        this.agendadoPara = janelaOtima;
        this.intensidadeJanelaGco2Kwh = intensidadeJanela;
        this.intensidadeBaselineGco2Kwh = intensidadeBaseline;
        this.status = JobStatus.AGENDADO;
    }

    public void marcarComoExecutado() {
        this.status = JobStatus.EXECUTADO;
    }

    /**
     * Carbono evitado (em gramas) por deslocar a carga para a janela limpa,
     * comparado a tê-la executado no momento da submissão.
     *
     * <p>gCO2 = (baseline − janela) [gCO2/kWh] × potência [kW] × duração [h]</p>
     */
    public double economiaEstimadaGramasCO2() {
        double horas = duracaoMinutos / 60.0;
        double delta = intensidadeBaselineGco2Kwh - intensidadeJanelaGco2Kwh;
        return Math.max(0, delta * consumoEstimadoKw * horas);
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
