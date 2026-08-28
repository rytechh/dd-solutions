package br.com.rytechh.greenshift.adapters.in.rest;

import br.com.rytechh.greenshift.core.domain.JobNaoAgendavelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz exceções de domínio em respostas HTTP, mantendo o core livre de
 * qualquer conhecimento sobre status codes.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Não existe janela viável dentro do deadline: o cliente precisa afrouxar o prazo. */
    @ExceptionHandler(JobNaoAgendavelException.class)
    public ProblemDetail jobNaoAgendavel(JobNaoAgendavelException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problema.setTitle("Job não agendável");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail payloadInvalido(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Payload inválido");
        problema.setTitle("Erro de validação");
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Requisição inválida");
        return problema;
    }
}
