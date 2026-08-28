package br.com.rytechh.greenshift.adapters.in.rest;

import br.com.rytechh.greenshift.adapters.in.rest.dto.DashboardResponse;
import br.com.rytechh.greenshift.adapters.in.rest.dto.ForecastResponse;
import br.com.rytechh.greenshift.application.ports.in.ConsultarJobPort;
import br.com.rytechh.greenshift.application.ports.in.ExecutarJobsPort;
import br.com.rytechh.greenshift.application.ports.out.CarbonForecastPort;
import br.com.rytechh.greenshift.application.ports.out.GreenMetricsRepositoryPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoints de apoio à demonstração: curva de carbono, painel de ESG e
 * gatilho manual do executor (para não precisar esperar o relógio na banca).
 */
@RestController
@RequestMapping("/api/v1")
public class GreenMetricsController {

    private final CarbonForecastPort carbonForecastPort;
    private final GreenMetricsRepositoryPort greenMetricsRepository;
    private final ConsultarJobPort consultarJobPort;
    private final ExecutarJobsPort executarJobsPort;

    public GreenMetricsController(CarbonForecastPort carbonForecastPort,
                                   GreenMetricsRepositoryPort greenMetricsRepository,
                                   ConsultarJobPort consultarJobPort,
                                   ExecutarJobsPort executarJobsPort) {
        this.carbonForecastPort = carbonForecastPort;
        this.greenMetricsRepository = greenMetricsRepository;
        this.consultarJobPort = consultarJobPort;
        this.executarJobsPort = executarJobsPort;
    }

    /** Curva de intensidade de carbono das próximas 24h. */
    @GetMapping("/forecast")
    public ForecastResponse forecast(@RequestParam(defaultValue = "BR") String regiao) {
        return ForecastResponse.de(regiao, carbonForecastPort.obterPrevisao24h(regiao));
    }

    /** Painel corporativo de ESG com o carbono acumulado que deixou de ser emitido. */
    @GetMapping("/metrics/dashboard")
    public DashboardResponse dashboard() {
        return DashboardResponse.de(consultarJobPort.listarTodos(), greenMetricsRepository.listarTodos());
    }

    /**
     * Antecipa o ciclo do executor. Sem parâmetro, considera o momento atual;
     * com {@code ate}, simula o avanço do relógio para a demo.
     */
    @PostMapping("/metrics/executar")
    public Map<String, Object> executarAgora(@RequestParam(required = false) Instant ate) {
        Instant momento = ate == null ? Instant.now() : ate;
        int executados = executarJobsPort.executarJobsDevidos(momento).size();
        return Map.of("momentoConsiderado", momento, "jobsExecutados", executados);
    }
}
