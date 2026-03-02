package org.bytefight.webserver.player.domain;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

import org.hibernate.validator.constraints.CompositionType;
import org.hibernate.validator.constraints.ConstraintComposition;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@ConstraintComposition(CompositionType.AND)
@NotBlank(message = "Username is required")
@Size(min = 3, max = 20, message = "Username must be 3–20 characters")
@Pattern(
    regexp = "^[A-Za-z0-9_]+$",
    message = "Username can only contain letters, numbers, and underscores")
public @interface PlayerUsername {
  String message() default "Invalid Username";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
