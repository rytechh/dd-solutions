package br.com.rytechh.greenshift.config;

import br.com.rytechh.greenshift.adapters.out.integration.SimuladoCarbonForecastAdapter;
import br.com.rytechh.greenshift.adapters.out.persistence.GreenMetricsJpaRepository;
import br.com.rytechh.greenshift.adapters.out.persistence.GreenMetricsRepositoryAdapter;
import br.com.rytechh.greenshift.adapters.out.persistence.JobRepositoryAdapter;
import br.com.rytechh.greenshift.adapters.out.persistence.WorkloadJobJpaRepository;
import br.com.rytechh.greenshift.application.ports.in.AgendarJobPort;
import br.com.rytechh.greenshift.application.ports.in.ConsultarJobPort;
import br.com.rytechh.greenshift.application.ports.in.ExecutarJobsPort;
import br.com.rytechh.greenshift.application.ports.out.CarbonForecastPort;
import br.com.rytechh.greenshift.application.ports.out.GreenMetricsRepositoryPort;
import br.com.rytechh.greenshift.application.ports.out.JobRepositoryPort;
import br.com.rytechh.greenshift.core.usecase.OtimizacaoEnergiaUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Único ponto onde o Spring encontra o CORE.
 *
 * <p>É aqui que as portas são plugadas nos adapters. Trocar a fonte de dados de
 * carbono (simulada → Electricity Maps) ou o banco (PostgreSQL → outro) é uma
 * mudança confinada a este arquivo: o motor de otimização não muda uma linha.</p>
 */
@Configuration
public class BeanConfig {

    @Bean
    public JobRepositoryPort jobRepositoryPort(WorkloadJobJpaRepository jpaRepository) {
        return new JobRepositoryAdapter(jpaRepository);
    }

    @Bean
    public GreenMetricsRepositoryPort greenMetricsRepositoryPort(GreenMetricsJpaRepository jpaRepository) {
        return new GreenMetricsRepositoryAdapter(jpaRepository);
    }

    @Bean
    public CarbonForecastPort carbonForecastPort() {
        return new SimuladoCarbonForecastAdapter();
    }

    /**
     * O motor de otimização, instanciado à mão para deixar explícito que ele é
     * uma classe Java pura — sem {@code @Service}, sem anotação alguma.
     */
    @Bean
    public OtimizacaoEnergiaUseCase otimizacaoEnergiaUseCase(JobRepositoryPort jobRepositoryPort,
                                                              CarbonForecastPort carbonForecastPort,
                                                              GreenMetricsRepositoryPort greenMetricsRepositoryPort) {
        return new OtimizacaoEnergiaUseCase(jobRepositoryPort, carbonForecastPort, greenMetricsRepositoryPort);
    }

    @Bean
    public AgendarJobPort agendarJobPort(OtimizacaoEnergiaUseCase useCase) {
        return useCase;
    }

    @Bean
    public ConsultarJobPort consultarJobPort(OtimizacaoEnergiaUseCase useCase) {
        return useCase;
    }

    @Bean
    public ExecutarJobsPort executarJobsPort(OtimizacaoEnergiaUseCase useCase) {
        return useCase;
    }
}
