package org.bytefight.webserver.gamematchfile.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFileVisibility;
import org.springframework.web.multipart.MultipartFile;

@Value
public class GameMatchFileUploadDto {
    @NotNull String gameMatchUuid;
    String teamUuid;
    @NotNull String slug;
    @NotNull MultipartFile file;
    @NotNull GameMatchFileVisibility visibility;
}
