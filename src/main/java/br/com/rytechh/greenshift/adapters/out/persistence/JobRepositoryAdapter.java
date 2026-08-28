package br.com.rytechh.greenshift.adapters.out.persistence;

import br.com.rytechh.greenshift.adapters.out.persistence.entity.WorkloadJobEntity;
import br.com.rytechh.greenshift.application.ports.out.JobRepositoryPort;
import br.com.rytechh.greenshift.core.domain.JobStatus;
import br.com.rytechh.greenshift.core.domain.WorkloadJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter de saída: traduz entre o domínio puro (WorkloadJob) e a entidade JPA
 * persistida no PostgreSQL. O core nunca vê WorkloadJobEntity.
 */
public class JobRepositoryAdapter implements JobRepositoryPort {

    private final WorkloadJobJpaRepository jpaRepository;

    public JobRepositoryAdapter(WorkloadJobJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public WorkloadJob salvar(WorkloadJob job) {
        jpaRepository.save(paraEntity(job));
        return job;
    }

    @Override
    public Optional<WorkloadJob> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(this::paraDominio);
    }

    @Override
    public List<WorkloadJob> listarTodos() {
        return jpaRepository.findAll().stream().map(this::paraDominio).toList();
    }

    @Override
    public List<WorkloadJob> buscarPorStatusEAgendadoAntesDe(JobStatus status, Instant momento) {
        return jpaRepository.findByStatusAndAgendadoParaLessThanEqual(status, momento).stream()
                .map(this::paraDominio)
                .toList();
    }

    private WorkloadJobEntity paraEntity(WorkloadJob job) {
        return new WorkloadJobEntity(job.getId(), job.getComando(), job.getDescricao(),
                job.getDuracaoMinutos(), job.getDeadline(), job.getConsumoEstimadoKw(),
                job.getCriadoEm(), job.getStatus(), job.getAgendadoPara(),
                job.getIntensidadeJanelaGco2Kwh(), job.getIntensidadeBaselineGco2Kwh());
    }

    private WorkloadJob paraDominio(WorkloadJobEntity entity) {
        return new WorkloadJob(entity.getId(), entity.getComando(), entity.getDescricao(),
                entity.getDuracaoMinutos(), entity.getDeadline(), entity.getConsumoEstimadoKw(),
                entity.getCriadoEm(), entity.getStatus(), entity.getAgendadoPara(),
                entity.getIntensidadeJanelaGco2Kwh(), entity.getIntensidadeBaselineGco2Kwh());
    }
}
