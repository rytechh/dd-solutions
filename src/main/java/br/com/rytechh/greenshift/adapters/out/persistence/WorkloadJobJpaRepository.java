package br.com.rytechh.greenshift.adapters.out.persistence;

import br.com.rytechh.greenshift.adapters.out.persistence.entity.WorkloadJobEntity;
import br.com.rytechh.greenshift.core.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WorkloadJobJpaRepository extends JpaRepository<WorkloadJobEntity, UUID> {

    List<WorkloadJobEntity> findByStatusAndAgendadoParaLessThanEqual(JobStatus status, Instant momento);
}
