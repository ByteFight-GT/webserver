package org.bytefight.webserver.user.infra;


import org.bytefight.webserver.storage.domain.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<FileRecord, Long> {
}
