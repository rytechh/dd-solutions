package br.com.rytechh.greenshift.core.usecase;

import br.com.rytechh.greenshift.application.ports.in.AgendarJobPort;
import br.com.rytechh.greenshift.application.ports.in.ConsultarJobPort;
import br.com.rytechh.greenshift.application.ports.in.ExecutarJobsPort;
import br.com.rytechh.greenshift.application.ports.out.CarbonForecastPort;
import br.com.rytechh.greenshift.application.ports.out.GreenMetricsRepositoryPort;
import br.com.rytechh.greenshift.application.ports.out.JobRepositoryPort;
import br.com.rytechh.greenshift.core.domain.CarbonForecast;
import br.com.rytechh.greenshift.core.domain.GreenMetrics;
import br.com.rytechh.greenshift.core.domain.JobNaoAgendavelException;
import br.com.rytechh.greenshift.core.domain.JobStatus;
import br.com.rytechh.greenshift.core.domain.WorkloadJob;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CORE: Motor de Otimização.
 *
 * <p>Classe pura de domínio — não depende de Spring, JPA ou de nenhum framework.
 * Recebe suas dependências (portas de saída) por construtor, conectadas em
 * tempo de execução pelo {@code config.BeanConfig}.</p>
 */
public class OtimizacaoEnergiaUseCase implements AgendarJobPort, ConsultarJobPort, ExecutarJobsPort {

    private static final String REGIAO_PADRAO = "BR";

    private final JobRepositoryPort jobRepository;
    private final CarbonForecastPort carbonForecastPort;
    private final GreenMetricsRepositoryPort greenMetricsRepository;

    public OtimizacaoEnergiaUseCase(JobRepositoryPort jobRepository,
                                     CarbonForecastPort carbonForecastPort,
                                     GreenMetricsRepositoryPort greenMetricsRepository) {
        this.jobRepository = jobRepository;
        this.carbonForecastPort = carbonForecastPort;
        this.greenMetricsRepository = greenMetricsRepository;
    }

    @Override
    public WorkloadJob agendar(NovoJobCommand command) {
        WorkloadJob job = WorkloadJob.novo(command.comando(), command.descricao(),
                command.duracaoMinutos(), command.deadline(), command.consumoEstimadoKw());

        List<CarbonForecast> previsao = ordenarPorTempo(carbonForecastPort.obterPrevisao24h(REGIAO_PADRAO));
        if (previsao.isEmpty()) {
            throw new JobNaoAgendavelException("Nenhuma previsão de carbono disponível para agendamento");
        }

        int slotsNecessarios = slotsNecessarios(job.getDuracaoMinutos());
        JanelaOtima janela = calcularMelhorJanela(previsao, slotsNecessarios, job.getDeadline());

        // Baseline honesto: o que custaria rodar a carga AGORA, sem deslocamento.
        double baseline = intensidadeMedia(previsao.subList(0, Math.min(slotsNecessarios, previsao.size())));

        job.agendarPara(janela.inicio(), janela.intensidadeMedia(), baseline);
        return jobRepository.salvar(job);
    }

    @Override
    public Optional<WorkloadJob> buscarPorId(UUID id) {
        return jobRepository.buscarPorId(id);
    }

    @Override
    public List<WorkloadJob> listarTodos() {
        return jobRepository.listarTodos();
    }

    /**
     * Efetiva os jobs cuja janela otimizada já chegou e registra, para cada um,
     * quanto carbono foi evitado em relação à execução imediata.
     */
    @Override
    public List<GreenMetrics> executarJobsDevidos(Instant agora) {
        List<WorkloadJob> devidos = jobRepository.buscarPorStatusEAgendadoAntesDe(JobStatus.AGENDADO, agora);
        List<GreenMetrics> registrados = new ArrayList<>();

        for (WorkloadJob job : devidos) {
            // A economia foi congelada no agendamento, quando a previsão era válida.
            double carbonoEvitado = job.economiaEstimadaGramasCO2();

            job.marcarComoExecutado();
            jobRepository.salvar(job);

            registrados.add(greenMetricsRepository.salvar(GreenMetrics.novo(job.getId(), agora, carbonoEvitado)));
        }
        return registrados;
    }

    /**
     * Desliza uma janela de N slots horários consecutivos (N = ceil(duração/60))
     * sobre a previsão de 24h e retorna a janela, com fim antes do deadline,
     * que apresenta a menor intensidade média de CO2.
     */
    private JanelaOtima calcularMelhorJanela(List<CarbonForecast> previsao, int slotsNecessarios, Instant deadline) {
        JanelaOtima melhor = null;

        for (int i = 0; i + slotsNecessarios <= previsao.size(); i++) {
            List<CarbonForecast> janela = previsao.subList(i, i + slotsNecessarios);
            Instant inicio = janela.get(0).timestamp();
            Instant fim = janela.get(janela.size() - 1).timestamp().plusSeconds(3600);

            if (fim.isAfter(deadline)) {
                continue;
            }

            double mediaIntensidade = intensidadeMedia(janela);
            if (melhor == null || mediaIntensidade < melhor.intensidadeMedia()) {
                melhor = new JanelaOtima(inicio, mediaIntensidade);
            }
        }

        if (melhor == null) {
            throw new JobNaoAgendavelException(
                    "Não há janela de %d hora(s) que caiba dentro do deadline informado".formatted(slotsNecessarios));
        }
        return melhor;
    }

    private static int slotsNecessarios(int duracaoMinutos) {
        return (int) Math.ceil(duracaoMinutos / 60.0);
    }

    private static double intensidadeMedia(List<CarbonForecast> slots) {
        return slots.stream().mapToDouble(CarbonForecast::co2Intensity).average().orElse(0);
    }

    private static List<CarbonForecast> ordenarPorTempo(List<CarbonForecast> previsao) {
        return previsao.stream().sorted(Comparator.comparing(CarbonForecast::timestamp)).toList();
    }

    private record JanelaOtima(Instant inicio, double intensidadeMedia) {
    }
}
