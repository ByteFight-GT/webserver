package org.bytefight.webserver.gamematchfile.domain.dto;

import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;

@Value
@Builder
public class GameMatchFileDto {
    @NotNull String uuid;
    @NotNull String gameMatchUuid;
    @NotNull String slug;
    @NotNull String fileRecordUuid;
    String teamUuid;
    @NotNull TimestampsDto timestamps;
    DownloadLinkDto downloadLink;

    public static GameMatchFileDto from(GameMatchFile gameMatchFile, DownloadLinkDto downloadLink) {
        return GameMatchFileDto.builder()
                .uuid(gameMatchFile.getUuid().toString())
                .gameMatchUuid(gameMatchFile.getGameMatch().getUuid().toString())
                .slug(gameMatchFile.getSlug())
                .fileRecordUuid(gameMatchFile.getFileRecord().getUuid().toString())
                .teamUuid(gameMatchFile.getTeam() != null ? gameMatchFile.getTeam().getUuid().toString() : null)
                .timestamps(TimestampsDto.from(gameMatchFile))
                .downloadLink(downloadLink)
                .build();
    }
}
