package br.com.rytechh.greenshift.adapters.in.scheduler;

import br.com.rytechh.greenshift.application.ports.in.ExecutarJobsPort;
import br.com.rytechh.greenshift.core.domain.GreenMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Adapter de entrada (agendamento): acorda periodicamente e pede ao core que
 * efetive os jobs cuja janela verde já chegou.
 */
@Component
public class ExecucaoJobsScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExecucaoJobsScheduler.class);

    private final ExecutarJobsPort executarJobsPort;

    public ExecucaoJobsScheduler(ExecutarJobsPort executarJobsPort) {
        this.executarJobsPort = executarJobsPort;
    }

    @Scheduled(fixedDelayString = "${greenshift.scheduler.intervalo-ms:60000}")
    public void executarJobsDevidos() {
        List<GreenMetrics> executados = executarJobsPort.executarJobsDevidos(Instant.now());
        if (executados.isEmpty()) {
            return;
        }
        double total = executados.stream().mapToDouble(GreenMetrics::getCarbonSavedGrams).sum();
        log.info("GreenShift executou {} job(s) na janela verde — {} gCO2 evitados",
                executados.size(), Math.round(total));
    }
}
