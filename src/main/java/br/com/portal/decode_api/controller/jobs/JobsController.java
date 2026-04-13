package br.com.portal.decode_api.controller.jobs;

import br.com.portal.decode_api.service.jobs.JobsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobsController {

    private final JobsService jobsService;

    @GetMapping
    public List<JobsService.JobRow> list(
            @RequestParam(required = false) String queue,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "200") int limit
    ) {
        return jobsService.list(queue, status, limit);
    }
}
