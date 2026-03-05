package org.bytefight.webserver.gamematchfile.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.bytefight.webserver.gamematchfile.domain.GameMatchFileVisibility;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameMatchFileUploadDto {
  @NotNull String gameMatchUuid;
  String teamUuid;
  @NotNull String slug;
  @NotNull MultipartFile file;
  @NotNull GameMatchFileVisibility visibility;
}
