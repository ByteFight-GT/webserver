package org.bytefight.webserver;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@SpringBootApplication
@RestController
@RequestMapping("/api/v1")
@EnableScheduling
@ConfigurationPropertiesScan
public class BotFightWebServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(BotFightWebServerApplication.class, args);
  }

  //    @Bean
  //    @ConditionalOnProperty(name = "search.index.enabled", havingValue = "true", matchIfMissing =
  // true)
  //    public ApplicationRunner buildIndex(SearchIndexBuild searchIndexBuild) {
  //        return args -> {searchIndexBuild.indexPersistedData();};
  //    }

  @GetMapping("/ping")
  public Map<String, String> ping() {
    return Map.of("ts", String.valueOf(System.currentTimeMillis()));
  }
}
