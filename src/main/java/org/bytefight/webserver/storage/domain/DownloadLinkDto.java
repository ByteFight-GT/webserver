package org.bytefight.webserver.storage.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.net.URI;

@Value
public class DownloadLinkDto {
  @NotNull URI uri;
  @NotNull long exp;
}
