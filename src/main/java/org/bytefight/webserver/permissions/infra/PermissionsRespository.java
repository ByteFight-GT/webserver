package org.bytefight.webserver.permissions.infra;

import org.bytefight.webserver.permissions.domain.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionsRespository extends JpaRepository<Permissions, Long> {

    public Permissions findTopByOrderByCreatedAtDesc();

}
