package br.com.rytechh.greenshift.application.ports.out;

import br.com.rytechh.greenshift.core.domain.JobStatus;
import br.com.rytechh.greenshift.core.domain.WorkloadJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída: persistência da fila de WorkloadJob. Implementada por um adapter
 * de infraestrutura (JPA/PostgreSQL) — o core não conhece esses detalhes.
 */
public interface JobRepositoryPort {

    WorkloadJob salvar(WorkloadJob job);

    Optional<WorkloadJob> buscarPorId(UUID id);

    List<WorkloadJob> listarTodos();

    List<WorkloadJob> buscarPorStatusEAgendadoAntesDe(JobStatus status, Instant momento);
}
