package org.bytefight.webserver.turnstile.infra;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(TurnstileProperties.class)
@RequiredArgsConstructor
public class TurnstileConfig implements WebMvcConfigurer {
  private final TurnstileInterceptor turnstileInterceptor;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(turnstileInterceptor).addPathPatterns("/**");
  }
}
