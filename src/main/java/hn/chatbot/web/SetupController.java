package hn.chatbot.web;

import hn.chatbot.service.setup.SetupService;
import hn.chatbot.service.setup.model.SetupRun;
import hn.chatbot.service.setup.model.SetupState;
import hn.chatbot.web.dto.SetupRunRequest;
import hn.chatbot.web.dto.SetupRunResponse;
import hn.chatbot.web.dto.SetupStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final SetupService setup;
    private final SetupSseBroadcaster broadcaster;

    public SetupController(SetupService setup, SetupSseBroadcaster broadcaster) {
        this.setup = setup;
        this.broadcaster = broadcaster;
    }

    @PostMapping("/run")
    public ResponseEntity<SetupRunResponse> run(@RequestBody SetupRunRequest request) {
        if (setup.progress().state() == SetupState.RUNNING) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        SetupRun run = setup.run(request.limit());
        return ResponseEntity.ok(new SetupRunResponse(run.runId(), run.state(), run.startedAt()));
    }

    /** 실행 중이 아닐 때 연결해도 현재 상태를 한 번 보내고 유지한다. */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return broadcaster.subscribe(setup.progress());
    }

    @GetMapping("/status")
    public SetupStatus status() {
        return SetupDtoMapper.toStatus(setup.progress());
    }

    @PostMapping("/stop")
    public Map<String, String> stop() {
        setup.stop();
        return Map.of("state", SetupState.STOPPED.name());
    }
}
