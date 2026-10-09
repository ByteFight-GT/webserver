package org.bytefight.webserver.globalstats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.globalstats.domain.GlobalStat;
import org.bytefight.webserver.globalstats.infra.GlobalStatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
class PublicGlobalStatsControllerIT extends FullStackIntegrationTestBase {
  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private GlobalStatRepository globalStatRepository;

  @Test
  void getGlobalStatsReturnsAllStatsAsDictionaryWithoutAuth() throws Exception {
    globalStatRepository.save(
        GlobalStat.builder().metric("total_matches_played").value(42L).build());
    globalStatRepository.save(GlobalStat.builder().metric("total_teams").value(7L).build());

    MvcResult result =
        mockMvc.perform(get("/api/v1/public/global-stats")).andExpect(status().isOk()).andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.size()).isEqualTo(2);
    assertThat(body.get("total_matches_played").asLong()).isEqualTo(42L);
    assertThat(body.get("total_teams").asLong()).isEqualTo(7L);
  }

  @Test
  void getGlobalStatsReturnsEmptyObjectWhenNoStatsExist() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/v1/public/global-stats")).andExpect(status().isOk()).andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.isObject()).isTrue();
    assertThat(body.size()).isZero();
  }
}
