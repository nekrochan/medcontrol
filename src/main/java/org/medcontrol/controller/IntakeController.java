package org.medcontrol.controller;

import jakarta.servlet.http.HttpSession;
import org.medcontrol.dto.request.IntakeDetailsRequestDto;
import org.medcontrol.entity.enums.IntakeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.medcontrol.dto.response.IntakeResponseDto;
import org.medcontrol.service.impl.IntakeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/intakes")
public class IntakeController {

    @Autowired
    private IntakeServiceImpl intakeService;
    private static final Logger log = LoggerFactory.getLogger(IntakeController.class);

    public IntakeController(IntakeServiceImpl intakeService) {
        this.intakeService = intakeService;
    }

    @GetMapping("/{intakeId}")
    public String getIntakeById(
            @PathVariable String intakeId,
            @RequestParam(required = false, defaultValue = "false") boolean nearest,
            @RequestParam(required = false) String returnDate,
            @RequestParam(required = false, defaultValue = "false") boolean editMode,
            Model model,
            Principal principal) {
        log.info("Получение деталей приема, id: {}, ближайший: {}, editMode: {}", intakeId, nearest, editMode);
        try {
            IntakeResponseDto intake = intakeService.getIntakeById(intakeId);
            IntakeDetailsRequestDto intakeDetails = intakeService.getIntakeDetailsWithAvailableStatuses(intakeId);

            model.addAttribute("intake", intake);
            model.addAttribute("availableStatuses", intakeDetails.getAvailableStatuses());
            model.addAttribute("isNearest", nearest);
            model.addAttribute("returnDate", returnDate);
            model.addAttribute("editMode", editMode);
            return "intakes/details";
        } catch (IllegalArgumentException e) {
            log.error("Прием не найден: {}", intakeId);
            return "error";
        }
    }

    @PostMapping("/{intakeId}/intakeStatus")
    public String updateStatus(
            @PathVariable String intakeId,
            @RequestParam String status,
            @RequestParam(required = false) String returnDate,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        log.info("Обновление статуса для приема: {}, новый статус: {}, дата: {}", intakeId, status, returnDate);

        try {
            IntakeResponseDto updated = intakeService.updateIntakeStatus(intakeId, status);
            redirectAttributes.addFlashAttribute(
                    "success", "Статус обновлен"
            );
            log.info("Статус успешно обновлен на: {}", updated.getIntakeStatus());
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса", e);
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Не удалось обновить статус: " + e.getMessage()
            );
        }

        if (returnDate != null && !returnDate.isEmpty()) {
            log.info("Редирект на: /intakes/date/{}", returnDate);
            return "redirect:/intakes/date/" + returnDate;
        }
        log.info("Редирект на: /intakes/{}", intakeId);
        return "redirect:/intakes/" + intakeId + "?returnDate=" + returnDate;
    }

    @PostMapping("/{intakeId}/move")
    public String moveIntake(
            @PathVariable String intakeId,
            @RequestParam(required = false) String returnDate,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        try {
            intakeService.moveIntakeForward(intakeId);
            redirectAttributes.addFlashAttribute(
                    "success", "Прием перенесен на 15 минут"
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (returnDate != null && !returnDate.isEmpty())
            return "redirect:/intakes/date/" + returnDate;
        return "redirect:/intakes/" + intakeId + "?returnDate=" + returnDate;
    }

    @PostMapping("/user/{profileId}/batch-move")
    public String batchMoveIntakes(
            @PathVariable String profileId,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        log.info("Перенос ближайших приемов для профиля: {}", profileId);
        try {
            List<IntakeResponseDto> movedIntakes = intakeService
                    .batchMoveIntakesForward(profileId);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Перенесено приемов: %d", movedIntakes.size()));
        } catch (Exception e) {
            log.error("Ошибка при массовом переносе приемов", e);
            redirectAttributes.addFlashAttribute("error",
                    "Не удалось перенести приемы: " + e.getMessage());
        }

        return "redirect:/intakes/date/" + LocalDate.now().toString();
    }

    @PostMapping("/{intakeId}/taken-time")
    public String updateTakenTime(
            @PathVariable String intakeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime takenAt,
            @RequestParam(required = false) String returnDate,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        log.info("Обновление времени приема для intake: {}", intakeId);
        try {
            IntakeResponseDto updated = intakeService.updateActualTime(intakeId, takenAt);
            redirectAttributes.addFlashAttribute("success", "Время приема обновлено");
        } catch (Exception e) {
            log.error("Ошибка при обновлении времени приема", e);
            redirectAttributes.addFlashAttribute("error", "Не удалось обновить время: " + e.getMessage());
        }
        if (returnDate != null && !returnDate.isEmpty())
            return "redirect:/intakes/date/" + returnDate;
        return "redirect:/intakes/" + intakeId + "?returnDate=" + returnDate;
    }

    @GetMapping
    public String redirectToTodayIntakes() {
        return "redirect:/intakes/date/" + LocalDate.now().toString();
    }

    @GetMapping("/date/{date}")
    public String getAllByProfileAndDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model,
            HttpSession session,
            @RequestParam(required = false) boolean forceRefresh,
            Principal principal) {
        String profileId = (String) session.getAttribute("activeProfileId");

        log.info("Получение приемов для профиля: {}, дата: {}", profileId, date);

        if (profileId == null) {
            log.warn("Нет активных профилей в сессии, перенаправление к /profiles");
            return "redirect:/user";
        }

        List<IntakeResponseDto> allDayIntakes;

        if (forceRefresh) {
            allDayIntakes = getIntakesWithRetry(profileId, date, 3);
        } else {
            allDayIntakes = intakeService.getIntakesForProfileAndDate(profileId, date);
        }

        List<IntakeResponseDto> nearestIntakes = intakeService.getNearestIntakes(profileId);
        List<String> nearestIntakeIds = nearestIntakes != null ?
                nearestIntakes.stream().map(IntakeResponseDto::getId).collect(Collectors.toList()) :
                List.of();

        boolean hasIntakes = allDayIntakes != null && !allDayIntakes.isEmpty();

        model.addAttribute("intakes", allDayIntakes != null ? allDayIntakes : List.of());
        model.addAttribute("nearestIntakeIds", nearestIntakeIds);
        model.addAttribute("profileId", profileId);
        model.addAttribute("date", date);
        model.addAttribute("hasIntakes", hasIntakes);

        if (!hasIntakes) {
            model.addAttribute("info", "Ничего не запланировано");
            log.info("Приемы для профиля: {} на дату: {} отсутствуют", profileId, date);
        }

        return "intakes/day";
    }

    @PostMapping("/user/{profileId}/batch-status")
    public String batchUpdateStatus(
            @PathVariable String profileId,
            @RequestParam String status,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        log.info("Обновление статуса для ближайших приемов профиля: {}, статус: {}",
                profileId, status);
        String currentDate = LocalDate.now().toString();
        try {
            IntakeStatus targetStatus = IntakeStatus.valueOf(status.toUpperCase());
            List<IntakeResponseDto> updatedIntakes = intakeService
                    .batchUpdateIntakeStatus(profileId, targetStatus);
            redirectAttributes.addFlashAttribute("success",
                    String.format("Обновлено приемов: %d", updatedIntakes.size()));
        } catch (Exception e) {
            log.error("Ошибка при групповом обновлении статуса", e);
            redirectAttributes.addFlashAttribute("error",
                    "Не удалось обновить статусы: " + e.getMessage());
        }

        return "redirect:/intakes/date/" + currentDate;
    }

    private List<IntakeResponseDto> getIntakesWithRetry(String profileId, LocalDate date, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            List<IntakeResponseDto> intakes = intakeService.getIntakesForProfileAndDate(profileId, date);
            if (intakes != null && !intakes.isEmpty()) {
                log.info("Приемы найдены после {} попытки", i + 1);
                return intakes;
            }
            log.info("Приемы не найдены, попытка {}/{}, ждем 100мс", i + 1, maxAttempts);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return intakeService.getIntakesForProfileAndDate(profileId, date);
    }
}
