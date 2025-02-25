package com.example.botfightwebserver.permissions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionsRespository extends JpaRepository<Permissions, Long> {

    public Permissions findTopByOrderByCreatedAtDesc();

}
