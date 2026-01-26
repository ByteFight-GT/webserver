package org.bytefight.webserver.team.infra;

import org.bytefight.webserver.team.application.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/team")
public class AdminTeamController {
    private final TeamService teamService;
}
