package org.bytefight.webserver.auth.infra;

import java.util.Optional;

import org.bytefight.webserver.auth.domain.SignupSurvey;
import org.bytefight.webserver.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignupSurveyRepository extends JpaRepository<SignupSurvey, Long> {
  Optional<SignupSurvey> findByUser(User user);

  boolean existsByUser(User user);
}
