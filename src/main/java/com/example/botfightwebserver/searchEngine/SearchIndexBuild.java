package com.example.botfightwebserver.searchEngine;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SearchIndexBuild {

    private final EntityManager entityManager;

    @Transactional
    public void indexPersistedData() throws IOException {
        try {
            log.info("Start building indexes");
            SearchSession searchSession = Search.session(entityManager);
            searchSession.massIndexer()
                .startAndWait();
            log.info("Finished building indexes");
        } catch (InterruptedException e) {
            log.error("Error during indexing data, " + e.getMessage());
            throw new IOException("Error during indexing data, " + e.getMessage());
        }
    }
}