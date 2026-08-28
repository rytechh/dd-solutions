package br.com.rytechh.greenshift.application.ports.out;

import br.com.rytechh.greenshift.core.domain.GreenMetrics;

import java.util.List;

/**
 * Porta de saída: persistência do histórico de auditoria ecológica (ESG).
 */
public interface GreenMetricsRepositoryPort {

    GreenMetrics salvar(GreenMetrics metrics);

    List<GreenMetrics> listarTodos();
}
