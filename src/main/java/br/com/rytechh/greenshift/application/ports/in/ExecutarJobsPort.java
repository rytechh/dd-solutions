package br.com.rytechh.greenshift.application.ports.in;

import br.com.rytechh.greenshift.core.domain.GreenMetrics;

import java.time.Instant;
import java.util.List;

/**
 * Porta de entrada: disparo da execução dos jobs cuja janela verde já chegou.
 * Acionada pelo adapter de scheduling (Spring {@code @Scheduled}) ou manualmente
 * pelo endpoint de demonstração.
 */
public interface ExecutarJobsPort {

    List<GreenMetrics> executarJobsDevidos(Instant agora);
}
