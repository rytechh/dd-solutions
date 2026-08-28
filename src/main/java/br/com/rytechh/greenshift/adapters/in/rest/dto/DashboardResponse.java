package br.com.rytechh.greenshift.adapters.in.rest.dto;

import br.com.rytechh.greenshift.core.domain.GreenMetrics;
import br.com.rytechh.greenshift.core.domain.JobStatus;
import br.com.rytechh.greenshift.core.domain.WorkloadJob;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Painel corporativo de ESG: consolida o carbono evitado por todas as cargas
 * já executadas na janela otimizada.
 */
public record DashboardResponse(
        long jobsRecebidos,
        Map<String, Long> jobsPorStatus,
        long jobsExecutados,
        double carbonoEvitadoGramas,
        double carbonoEvitadoKg,
        double equivalenteArvoresPlantadas,
        List<Registro> historico
) {
    /** Uma árvore absorve ~21 kg de CO2 por ano (referência EPA/IPCC). */
    private static final double KG_CO2_POR_ARVORE_ANO = 21.0;

    public record Registro(Instant executadoEm, double carbonoEvitadoGramas) {
    }

    public static DashboardResponse de(List<WorkloadJob> jobs, List<GreenMetrics> metricas) {
        Map<String, Long> porStatus = jobs.stream()
                .collect(Collectors.groupingBy(j -> j.getStatus().name(), Collectors.counting()));

        for (JobStatus status : JobStatus.values()) {
            porStatus.putIfAbsent(status.name(), 0L);
        }

        double totalGramas = metricas.stream().mapToDouble(GreenMetrics::getCarbonSavedGrams).sum();
        double totalKg = totalGramas / 1000.0;

        List<Registro> historico = metricas.stream()
                .sorted(java.util.Comparator.comparing(GreenMetrics::getExecutedAt).reversed())
                .map(m -> new Registro(m.getExecutedAt(), arredondar(m.getCarbonSavedGrams())))
                .toList();

        return new DashboardResponse(
                jobs.size(),
                porStatus,
                metricas.size(),
                arredondar(totalGramas),
                arredondar(totalKg),
                arredondar(totalKg / KG_CO2_POR_ARVORE_ANO),
                historico);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
