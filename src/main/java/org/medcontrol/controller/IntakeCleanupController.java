package org.medcontrol.controller;

import org.medcontrol.dto.request.IntakeCleanupRequestDto;
import org.medcontrol.dto.response.IntakeCleanupResponseDto;
import org.medcontrol.service.IntakeCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/intakes/cleanup")
public class IntakeCleanupController {

    private static final Logger log = LoggerFactory.getLogger(IntakeCleanupController.class);

    @Autowired
    private IntakeCleanupService intakeCleanupService;

    @PostMapping("/after-schedule-update")
    public ResponseEntity<IntakeCleanupResponseDto> cleanupAfterScheduleUpdate(
            @RequestBody IntakeCleanupRequestDto request) {

        log.info("Запрос на очистку приемов для схемы: {}", request.getSchemeId());

        try {
            IntakeCleanupResponseDto response = intakeCleanupService.cleanupDuplicateIntakes(
                    request.getSchemeId(),
                    request.getNewAlarmTimes()
            );

            log.info("Очистка завершена. Удалено (помечено DELETING): {}", response.getDeletedCount());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при очистке приемов: ", e);
            IntakeCleanupResponseDto errorResponse = new IntakeCleanupResponseDto();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}