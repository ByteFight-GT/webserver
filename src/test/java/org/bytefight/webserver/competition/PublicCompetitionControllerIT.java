package org.bytefight.webserver.competition;

import org.bytefight.webserver.FullStackIntegrationTestBase;
import org.bytefight.webserver.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PublicCompetitionControllerIT extends FullStackIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void getAllCompetitionsReturnsEmptyListWhenNoCompetitions() throws Exception {
        mockMvc.perform(get("/api/v1/public/competition/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllCompetitionsReturnsAllCompetitions() throws Exception {
        testDataFactory.createCompetition("comp-1", "Competition One", true, 2);
        testDataFactory.createCompetition("comp-2", "Competition Two", false, 3);

        mockMvc.perform(get("/api/v1/public/competition/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug").isArray())
                .andExpect(jsonPath("$[*].slug").value(org.hamcrest.Matchers.hasItems("comp-1", "comp-2")));
    }
}
