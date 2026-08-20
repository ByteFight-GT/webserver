package org.bytefight.webserver.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.common.domain.BaseEntity;
import org.bytefight.webserver.storage.domain.FileRecord;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {
  @Column(name = "uuid", nullable = false, unique = true)
  private UUID uuid;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  /** Is this user an admin? */
  @Column(name = "is_admin", nullable = false)
  private boolean isAdmin = false;

  /** Is this user a service account? (less powerful than admin) */
  @Column(name = "is_service_account", nullable = false)
  private boolean isServiceAccount = false;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resume_file", unique = true)
  private FileRecord resume;

  public boolean isAdminOrServiceAccount() {
    return isAdmin || isServiceAccount;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public String getPassword() {
    return "";
  }
}
