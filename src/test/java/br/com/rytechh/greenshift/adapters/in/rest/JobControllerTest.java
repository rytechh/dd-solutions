package br.com.rytechh.greenshift.adapters.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração ponta a ponta: sobe o contexto completo (REST + core + JPA/H2)
 * e valida o contrato descrito na documentação do MVP.
 */
@SpringBootTest
class JobControllerTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    @DisplayName("POST /api/v1/jobs agenda a carga e devolve a janela verde escolhida")
    void criaEAgendaJob() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "comando", "pg_dump producao",
                "descricao", "backup diario",
                "duracaoMinutos", 120,
                "deadline", Instant.now().plus(20, ChronoUnit.HOURS).toString(),
                "consumoEstimadoKw", 12.5));

        String body = mockMvc().perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.agendadoPara").exists())
                .andExpect(jsonPath("$.economiaEstimadaGramasCO2").isNumber())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc().perform(get("/api/v1/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comando").value("pg_dump producao"));
    }

    @Test
    @DisplayName("POST /api/v1/jobs rejeita payload sem comando")
    void rejeitaPayloadInvalido() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "comando", "",
                "duracaoMinutos", 60,
                "deadline", Instant.now().plus(5, ChronoUnit.HOURS).toString()));

        mockMvc().perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/jobs devolve 422 quando nenhuma janela cabe no deadline")
    void recusaJobSemJanelaViavel() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "comando", "job impossivel",
                "duracaoMinutos", 600,
                "deadline", Instant.now().plus(1, ChronoUnit.HOURS).toString()));

        mockMvc().perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /api/v1/forecast expõe a curva de carbono das proximas 24h")
    void exibeCurvaDeCarbono() throws Exception {
        mockMvc().perform(get("/api/v1/forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curva.length()").value(24))
                .andExpect(jsonPath("$.melhorJanela").exists());
    }

    @Test
    @DisplayName("GET /api/v1/metrics/dashboard responde o painel de ESG")
    void exibeDashboard() throws Exception {
        mockMvc().perform(get("/api/v1/metrics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carbonoEvitadoGramas").isNumber())
                .andExpect(jsonPath("$.jobsPorStatus").exists());
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} devolve 404 para id inexistente")
    void jobInexistente() throws Exception {
        mockMvc().perform(get("/api/v1/jobs/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
