package br.com.rytechh.greenshift.core.usecase;

import br.com.rytechh.greenshift.application.ports.in.AgendarJobPort.NovoJobCommand;
import br.com.rytechh.greenshift.application.ports.out.CarbonForecastPort;
import br.com.rytechh.greenshift.application.ports.out.GreenMetricsRepositoryPort;
import br.com.rytechh.greenshift.application.ports.out.JobRepositoryPort;
import br.com.rytechh.greenshift.core.domain.CarbonForecast;
import br.com.rytechh.greenshift.core.domain.GreenMetrics;
import br.com.rytechh.greenshift.core.domain.JobNaoAgendavelException;
import br.com.rytechh.greenshift.core.domain.JobStatus;
import br.com.rytechh.greenshift.core.domain.WorkloadJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do motor de otimização usando dublês em memória.
 *
 * <p>Nenhum {@code @SpringBootTest}, nenhum banco: por ser um núcleo isolado por
 * portas, a regra de sustentabilidade é testável em milissegundos.</p>
 */
class OtimizacaoEnergiaUseCaseTest {

    private Instant agora;
    private JobRepositoryMemoria jobRepository;
    private GreenMetricsRepositoryMemoria metricsRepository;

    @BeforeEach
    void setUp() {
        agora = Instant.now().truncatedTo(ChronoUnit.HOURS);
        jobRepository = new JobRepositoryMemoria();
        metricsRepository = new GreenMetricsRepositoryMemoria();
    }

    @Test
    @DisplayName("agenda o job na hora de menor intensidade de carbono dentro do deadline")
    void agendaNoValeDeCarbono() {
        // Vale de emissão na terceira hora (100 gCO2/kWh).
        var useCase = novoUseCase(previsaoCom(500, 400, 100, 300));

        WorkloadJob job = useCase.agendar(new NovoJobCommand(
                "pg_dump producao", "backup noturno", 60, agora.plus(4, ChronoUnit.HOURS), 10));

        assertEquals(agora.plus(2, ChronoUnit.HOURS), job.getAgendadoPara());
        assertEquals(JobStatus.AGENDADO, job.getStatus());
        assertEquals(100.0, job.getIntensidadeJanelaGco2Kwh());
    }

    @Test
    @DisplayName("respeita o deadline mesmo que o melhor vale esteja depois dele")
    void naoUltrapassaODeadline() {
        // O vale real (50) está na hora 3, mas o deadline só permite até a hora 2.
        var useCase = novoUseCase(previsaoCom(500, 200, 400, 50));

        WorkloadJob job = useCase.agendar(new NovoJobCommand(
                "etl diario", null, 60, agora.plus(2, ChronoUnit.HOURS), 10));

        assertEquals(agora.plus(1, ChronoUnit.HOURS), job.getAgendadoPara());
        assertTrue(job.getAgendadoPara().isBefore(job.getDeadline()));
    }

    @Test
    @DisplayName("aloca janelas de múltiplas horas para jobs longos")
    void agendaJobDeMultiplasHoras() {
        // Melhor par consecutivo é (100, 120) começando na hora 2.
        var useCase = novoUseCase(previsaoCom(500, 400, 100, 120, 600));

        WorkloadJob job = useCase.agendar(new NovoJobCommand(
                "retreino modelo", null, 120, agora.plus(5, ChronoUnit.HOURS), 10));

        assertEquals(agora.plus(2, ChronoUnit.HOURS), job.getAgendadoPara());
        assertEquals(110.0, job.getIntensidadeJanelaGco2Kwh());
    }

    @Test
    @DisplayName("recusa o job quando nenhuma janela cabe antes do deadline")
    void recusaQuandoNaoHaJanela() {
        var useCase = novoUseCase(previsaoCom(500, 400, 100));

        var comando = new NovoJobCommand(
                "job impossivel", null, 180, agora.plus(1, ChronoUnit.HOURS), 10);

        assertThrows(JobNaoAgendavelException.class, () -> useCase.agendar(comando));
    }

    @Test
    @DisplayName("calcula o carbono evitado comparando a janela verde com a execução imediata")
    void calculaCarbonoEvitado() {
        // Baseline = 500 gCO2/kWh (agora) e janela escolhida = 100. Delta = 400.
        var useCase = novoUseCase(previsaoCom(500, 400, 100));

        WorkloadJob job = useCase.agendar(new NovoJobCommand(
                "backup", null, 60, agora.plus(3, ChronoUnit.HOURS), 10));

        // 400 gCO2/kWh x 10 kW x 1 h = 4000 g
        assertEquals(4000.0, job.economiaEstimadaGramasCO2());
    }

    @Test
    @DisplayName("executa os jobs vencidos e registra a métrica de auditoria ESG")
    void executaJobsDevidosERegistraMetrica() {
        var useCase = novoUseCase(previsaoCom(500, 400, 100));

        WorkloadJob job = useCase.agendar(new NovoJobCommand(
                "backup", null, 60, agora.plus(3, ChronoUnit.HOURS), 10));

        List<GreenMetrics> registradas = useCase.executarJobsDevidos(job.getAgendadoPara());

        assertEquals(1, registradas.size());
        assertEquals(4000.0, registradas.get(0).getCarbonSavedGrams());
        assertEquals(JobStatus.EXECUTADO, jobRepository.buscarPorId(job.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("não reexecuta um job que já foi executado")
    void naoReexecutaJob() {
        var useCase = novoUseCase(previsaoCom(500, 400, 100));

        WorkloadJob job = useCase.agendar(new NovoJobCommand(
                "backup", null, 60, agora.plus(3, ChronoUnit.HOURS), 10));

        useCase.executarJobsDevidos(job.getAgendadoPara());
        List<GreenMetrics> segundaRodada = useCase.executarJobsDevidos(job.getAgendadoPara());

        assertTrue(segundaRodada.isEmpty());
    }

    // ---------- infraestrutura de teste ----------

    private OtimizacaoEnergiaUseCase novoUseCase(List<CarbonForecast> previsao) {
        return new OtimizacaoEnergiaUseCase(jobRepository, regiao -> previsao, metricsRepository);
    }

    /** Constrói uma previsão horária a partir da hora atual, uma intensidade por hora. */
    private List<CarbonForecast> previsaoCom(double... intensidades) {
        List<CarbonForecast> previsao = new ArrayList<>();
        for (int i = 0; i < intensidades.length; i++) {
            previsao.add(new CarbonForecast(agora.plus(i, ChronoUnit.HOURS), intensidades[i], "BR"));
        }
        return previsao;
    }

    private static class JobRepositoryMemoria implements JobRepositoryPort {
        private final Map<UUID, WorkloadJob> banco = new HashMap<>();

        @Override
        public WorkloadJob salvar(WorkloadJob job) {
            banco.put(job.getId(), job);
            return job;
        }

        @Override
        public Optional<WorkloadJob> buscarPorId(UUID id) {
            return Optional.ofNullable(banco.get(id));
        }

        @Override
        public List<WorkloadJob> listarTodos() {
            return List.copyOf(banco.values());
        }

        @Override
        public List<WorkloadJob> buscarPorStatusEAgendadoAntesDe(JobStatus status, Instant momento) {
            return banco.values().stream()
                    .filter(j -> j.getStatus() == status)
                    .filter(j -> j.getAgendadoPara() != null && !j.getAgendadoPara().isAfter(momento))
                    .toList();
        }
    }

    private static class GreenMetricsRepositoryMemoria implements GreenMetricsRepositoryPort {
        private final List<GreenMetrics> banco = new ArrayList<>();

        @Override
        public GreenMetrics salvar(GreenMetrics metrics) {
            banco.add(metrics);
            return metrics;
        }

        @Override
        public List<GreenMetrics> listarTodos() {
            return List.copyOf(banco);
        }
    }
}
