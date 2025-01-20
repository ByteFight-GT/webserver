package com.example.botfightwebserver;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class PersistentTestBase {

    @Autowired
    private EntityManager entityManager;

    public <T> void persistEntity(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
    }

    public <T> T persistAndReturnEntity(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }
}
