package br.com.rytechh.greenshift.adapters.in.rest.dto;

import br.com.rytechh.greenshift.core.domain.JobStatus;
import br.com.rytechh.greenshift.core.domain.WorkloadJob;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String comando,
        String descricao,
        int duracaoMinutos,
        Instant deadline,
        double consumoEstimadoKw,
        Instant criadoEm,
        JobStatus status,
        Instant agendadoPara,
        Long deslocamentoHoras,
        Double intensidadeJanelaGco2Kwh,
        Double intensidadeBaselineGco2Kwh,
        Double economiaEstimadaGramasCO2,
        Double reducaoPercentual
) {
    public static JobResponse de(WorkloadJob job) {
        Long deslocamento = job.getAgendadoPara() == null
                ? null
                : Duration.between(job.getCriadoEm(), job.getAgendadoPara()).toHours();

        double baseline = job.getIntensidadeBaselineGco2Kwh();
        Double reducao = baseline <= 0
                ? null
                : arredondar((baseline - job.getIntensidadeJanelaGco2Kwh()) / baseline * 100);

        return new JobResponse(
                job.getId(),
                job.getComando(),
                job.getDescricao(),
                job.getDuracaoMinutos(),
                job.getDeadline(),
                job.getConsumoEstimadoKw(),
                job.getCriadoEm(),
                job.getStatus(),
                job.getAgendadoPara(),
                deslocamento,
                arredondar(job.getIntensidadeJanelaGco2Kwh()),
                arredondar(baseline),
                arredondar(job.economiaEstimadaGramasCO2()),
                reducao);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
