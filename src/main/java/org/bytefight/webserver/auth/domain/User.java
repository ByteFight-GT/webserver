package org.bytefight.webserver.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bytefight.webserver.common.domain.BaseEntity;
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

  @Column(name = "is_admin", nullable = false)
  private boolean isAdmin = false;

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
