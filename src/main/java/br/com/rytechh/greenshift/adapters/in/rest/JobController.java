package br.com.rytechh.greenshift.adapters.in.rest;

import br.com.rytechh.greenshift.adapters.in.rest.dto.CreateJobRequest;
import br.com.rytechh.greenshift.adapters.in.rest.dto.JobResponse;
import br.com.rytechh.greenshift.application.ports.in.AgendarJobPort;
import br.com.rytechh.greenshift.application.ports.in.ConsultarJobPort;
import br.com.rytechh.greenshift.core.domain.WorkloadJob;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Adapter de entrada (REST). Só traduz HTTP &lt;-&gt; portas de entrada;
 * nenhuma regra de negócio mora aqui.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final AgendarJobPort agendarJobPort;
    private final ConsultarJobPort consultarJobPort;

    public JobController(AgendarJobPort agendarJobPort, ConsultarJobPort consultarJobPort) {
        this.agendarJobPort = agendarJobPort;
        this.consultarJobPort = consultarJobPort;
    }

    /** Recebe uma carga de trabalho e devolve a janela verde escolhida pelo motor. */
    @PostMapping
    public ResponseEntity<JobResponse> criar(@Valid @RequestBody CreateJobRequest request) {
        WorkloadJob job = agendarJobPort.agendar(new AgendarJobPort.NovoJobCommand(
                request.comando(),
                request.descricao(),
                request.duracaoMinutos(),
                request.deadline(),
                request.consumoEstimadoKw() == null ? 0 : request.consumoEstimadoKw()));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(job.getId())
                .toUri();

        return ResponseEntity.created(location).body(JobResponse.de(job));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> buscar(@PathVariable UUID id) {
        return consultarJobPort.buscarPorId(id)
                .map(job -> ResponseEntity.ok(JobResponse.de(job)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<JobResponse> listar() {
        return consultarJobPort.listarTodos().stream().map(JobResponse::de).toList();
    }
}
