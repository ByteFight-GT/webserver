package org.bytefight.webserver.user.domain.dto;

import com.google.api.services.storage.Storage;
import org.bytefight.webserver.auth.domain.User;
import org.bytefight.webserver.common.domain.dto.TimestampsDto;
import org.bytefight.webserver.gamematchfile.domain.GameMatchFile;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.bytefight.webserver.storage.domain.DownloadLinkDto;
import org.bytefight.webserver.storage.domain.FileRecord;

@Value
@Builder
public class ResumeDto {
    @NotNull String uuid;
    @NotNull String fileRecordUuid;
    DownloadLinkDto downloadLink;

    public static ResumeDto from(DownloadLinkDto downloadLink, User user) {
        return ResumeDto.builder()
                .uuid(user.getUuid().toString())
                .fileRecordUuid(user.getResume().toString())
                .downloadLink(downloadLink)
                .build();
    }
}
