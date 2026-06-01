package org.medcontrol.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.medcontrol.dto.request.CreateSchemeDto;
import org.medcontrol.dto.request.SchemeDateTimeRequestDto;
import org.medcontrol.dto.request.SchemeSimpleDataRequestDto;
import org.medcontrol.dto.request.SchemeStatusRequestDto;
import org.medcontrol.dto.response.ProfileStatisticResponseDto;
import org.medcontrol.dto.response.SchemeResponseDto;
import org.medcontrol.dto.response.SchemeStatisticResponseDto;
import org.medcontrol.entity.Scheme;
import org.medcontrol.entity.enums.SchemeType;
import org.medcontrol.entity.keepers.AlternationDaysKeeper;
import org.medcontrol.entity.keepers.WeekdaysKeeper;
import org.medcontrol.service.impl.ProfileServiceImpl;
import org.medcontrol.service.impl.SchemeServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/schemes")
public class SchemeController {

    private static final Logger log = LoggerFactory.getLogger(SchemeController.class);
    private final SchemeServiceImpl schemeService;
    private final ProfileServiceImpl profileService;

    @Autowired
    public SchemeController(SchemeServiceImpl schemeService, ProfileServiceImpl profileService) {
        this.schemeService = schemeService;
        this.profileService = profileService;
    }

    @GetMapping("/add")
    public String addScheme(Model model, HttpSession session, Principal principal) {
        log.info("Контроллер: гет-запрос на создание схемы");
        CreateSchemeDto createSchemeDto = new CreateSchemeDto();

        Object profileIdObj = session.getAttribute("activeProfileId");
        if (profileIdObj != null) {
            createSchemeDto.setProfileId(profileIdObj.toString());
        } else {
            log.info("Исключение на уровне контроллера: гет-запрос на создание схемы: Профиль не выбран");
            throw new IllegalStateException("Профиль не выбран");
        }

        model.addAttribute("schemeModel", createSchemeDto);
        log.info("Контроллер: гет-запрос на создание схемы - успешно");
        return "scheme-add";
    }

    @ModelAttribute("schemeModel")
    public CreateSchemeDto initScheme() {
        return new CreateSchemeDto();
    }

    @PostMapping("/add")
    public String addScheme(
            @Valid CreateSchemeDto schemeModel,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Principal principal
    ) {
        log.info("Контроллер: пост-запрос на создание схемы");
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("schemeModel", schemeModel);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.schemeModel",
                    bindingResult);
            return "redirect:/schemes/add";
        }

        log.info("Контроллер: пост-запрос на создание схемы - успешно");
        Scheme scheme = schemeService.createScheme(schemeModel);
        return "redirect:/schemes/" + scheme.getId() + "/update-schedule";
    }

    @GetMapping("/{schemeId}/update-info")
    public String updateSchemeInfo(
            @PathVariable("schemeId") String schemeId,
            Model model,
            Principal principal
    ) {
        try {
            SchemeResponseDto schemeDto = schemeService.getSchemeDtoById(schemeId);
            model.addAttribute("schemeModel", schemeDto);
            return "scheme-update-info";
        } catch (IllegalArgumentException e) {
            return "redirect:/schemes/all";
        }
    }

    @PostMapping("/{schemeId}/update-info")
    public String updateSchemeInfo(
            @PathVariable("schemeId") String schemeId,
            @Valid SchemeSimpleDataRequestDto schemeSimpleDataDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Principal principal
    ) {
        schemeSimpleDataDto.setSchemeId(schemeId);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("schemeModel", schemeSimpleDataDto);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.schemeModel",
                    bindingResult);
            return "redirect:/schemes/" + schemeId + "/update-info";
        }

        schemeService.updateSchemeSimpleData(schemeSimpleDataDto);
        return "redirect:/schemes/" + schemeId;
    }

    @GetMapping("/{schemeId}/update-schedule")
    public String updateSchemeSchedule(
            @PathVariable("schemeId") String schemeId,
            Model model,
            Principal principal
    ) {
        try {
            SchemeResponseDto schemeDto = schemeService.getSchemeDtoById(schemeId);

            if (schemeDto.getAlternationDays() == null) {
                schemeDto.setAlternationDays(new AlternationDaysKeeper(1, 0));
            }
            if (schemeDto.getWeekdays() == null) {
                schemeDto.setWeekdays(new WeekdaysKeeper(
                        false, false, false,
                        false, false, false, false
                ));
            }

            log.info("Scheme data: startDate={}, alarmTimes={}, schemeType={}",
                    schemeDto.getStartDate(),
                    schemeDto.getAlarmTimes(),
                    schemeDto.getSchemeType());

            if (schemeDto.getAlarmTimes() == null) {
                schemeDto.setAlarmTimes(Collections.emptyList());
            }

            model.addAttribute("schemeModel", schemeDto);
            return "scheme-update-schedule";
        } catch (Exception e) {
            log.error("Ошибка при отображении формы расписания: ", e);
            return "redirect:/schemes/" + schemeId;
        }
    }

    @PostMapping("/{schemeId}/update-schedule")
    public String updateSchemeSchedule(
            @PathVariable("schemeId") String schemeId,
            @Valid SchemeDateTimeRequestDto schemeDateTimeRequestDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Principal principal
    ) {
        log.info("ВЫЗВАН МЕТОД: SchemeController.updateSchemeSchedule() - @PostMapping");
        schemeDateTimeRequestDto.setSchemeId(schemeId);

        String schemeType = request.getParameter("schemeType");

        if (schemeType == null || schemeType.trim().isEmpty()) {
            Scheme existingScheme = schemeService.getSchemeById(schemeId);
            schemeType = existingScheme.getSchemeType().name();
            log.info("Тип схемы не передан в параметрах, используем существующий: {}", schemeType);
        }

        String[] notificationTimes = request.getParameterValues("notificationTimes");
        if (notificationTimes != null && notificationTimes.length > 0) {
            List<LocalTime> alarms = new ArrayList<>();
            for (String timeStr : notificationTimes) {
                if (timeStr != null && !timeStr.trim().isEmpty()) {
                    alarms.add(LocalTime.parse(timeStr.trim()));
                }
            }
            schemeDateTimeRequestDto.setAlarmsPerDay(alarms);
        }

        if (SchemeType.WEEKDAYS.toString().equals(schemeType)) {
            String selectedDaysStr = request.getParameter("selectedDays");
            if (selectedDaysStr != null && !selectedDaysStr.trim().isEmpty()) {
                String[] dayArray = selectedDaysStr.split(",");
                List<String> validDays = Arrays.stream(dayArray)
                        .filter(day -> day != null && !day.trim().isEmpty())
                        .collect(Collectors.toList());

                if (!validDays.isEmpty()) {
                    WeekdaysKeeper weekdaysKeeper = convertToWeekdaysKeeper(validDays.toArray(new String[0]));
                    schemeDateTimeRequestDto.setWeekdays(weekdaysKeeper);
                    schemeDateTimeRequestDto.setSchemeType(SchemeType.WEEKDAYS.toString());
                } else {
                    bindingResult.rejectValue("weekdays", "error.weekdays", "Выберите хотя бы один день недели");
                }
            } else {
                bindingResult.rejectValue("weekdays", "error.weekdays", "Выберите хотя бы один день недели");
            }
        }
        else if (SchemeType.ALTERNATION.toString().equals(schemeType)) {
            String takeDaysStr = request.getParameter("takeDays");
            String skipDaysStr = request.getParameter("skipDays");

            log.info("ALTERNATION параметры: takeDays={}, skipDays={}", takeDaysStr, skipDaysStr);

            if (takeDaysStr != null && !takeDaysStr.isEmpty()) {
                int takeDays = Integer.parseInt(takeDaysStr);
                int skipDays = Integer.parseInt(skipDaysStr);

                log.info("Парсинг ALTERNATION: takeDays={}, skipDays={}", takeDays, skipDays);

                AlternationDaysKeeper daysKeeper = new AlternationDaysKeeper();
                daysKeeper.setTakeDays(takeDays);
                daysKeeper.setSkipDays(skipDays);
                log.info("AlternationDaysKeeper: takeDays={}, skipDays={}",
                        daysKeeper.getTakeDays(), daysKeeper.getSkipDays()
                );
                schemeDateTimeRequestDto.setAlternationDays(daysKeeper);
                log.info("В основной ветке: " +
                                "в schemeDateTimeRequestDto записано: SchemeType={}, takeDays={}, skipDays={}",
                        schemeDateTimeRequestDto.getSchemeType(),
                        schemeDateTimeRequestDto.getAlternationDays().getTakeDays(),
                        schemeDateTimeRequestDto.getAlternationDays().getSkipDays());
            } else {
                log.warn("Параметры ALTERNATION не найдены, устанавливаем значения по умолчанию");
                AlternationDaysKeeper alternationDays = new AlternationDaysKeeper(1, 0);
                schemeDateTimeRequestDto.setAlternationDays(alternationDays);
                schemeDateTimeRequestDto.setSchemeType(SchemeType.ALTERNATION.toString());
            }
        }

        if (bindingResult.hasErrors()) {
            log.warn("updateSchemeSchedule() - bindingResult.hasErrors(): ");
            try {
                SchemeResponseDto schemeDto = schemeService.getSchemeDtoById(schemeId);
                log.warn("Создано SchemeResponseDto для схемы, полученной из репозитория");
                redirectAttributes.addFlashAttribute("schemeModel", schemeDto);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("schemeModel", schemeDateTimeRequestDto);
            }
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.schemeModel",
                    bindingResult);
            return "redirect:/schemes/" + schemeId + "/update-schedule";
        }

        log.info("""
                SchemeController.updateSchemeSchedule() -->> SchemeService.createScheme():
                Параметры, передаваемые через SchemeDateTimeRequestDto:
                SchemeType={}, takeDays={}, skipDays={}, weekdays={}
                """,
                schemeDateTimeRequestDto.getSchemeType(),
                schemeDateTimeRequestDto.getAlternationDays().getTakeDays(),
                schemeDateTimeRequestDto.getAlternationDays().getSkipDays(),
                schemeDateTimeRequestDto.getWeekdays().toList()
        );
        schemeService.updateSchemeDateTime(schemeDateTimeRequestDto);
        redirectAttributes.addFlashAttribute("forceRefresh", "true");

        log.info("ЗАВЕРШЕН МЕТОД: SchemeController.updateSchemeSchedule() - @PostMapping");
        return "redirect:/intakes/date/" + LocalDate.now().toString();
    }

    @PostMapping("/{schemeId}/update-status")
    public String updateSchemeStatus(
            @PathVariable("schemeId") String schemeId,
            @Valid SchemeStatusRequestDto schemeStatusRequestDto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Principal principal
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("schemeModel", schemeStatusRequestDto);
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.schemeModel",
                    bindingResult);
            return "redirect:/schemes/" + schemeId;
        }

        schemeService.updateSchemeStatus(schemeId, schemeStatusRequestDto);
        return "redirect:/schemes/" + schemeId;
    }

    @PostMapping("/{schemeId}/delete")
    public String deleteScheme(@PathVariable("schemeId") String schemeId, Principal principal) {
        schemeService.deleteScheme(schemeId);
        return "redirect:/schemes/all";
    }

    @GetMapping("/all")
    public String showAllSchemes(Model model, HttpSession session, Principal principal) {
        String profileId = (String) session.getAttribute("activeProfileId");

        if (profileId == null || profileId.isEmpty()) {
            throw new IllegalStateException("Профиль не выбран. Пожалуйста, выберите профиль.");
        }

        ProfileStatisticResponseDto statistics = profileService.getProfileStatistics(profileId);
        List<SchemeResponseDto> schemes = schemeService.getSchemesByProfileId(profileId);

        model.addAttribute("allSchemes", schemes);
        model.addAttribute("statistics", statistics);

        return "scheme-all";
    }

    @GetMapping("/{schemeId}")
    public String getSchemeById(
            @PathVariable("schemeId") String schemeId,
            Model model,
            Principal principal
    ) {
        try {
            SchemeResponseDto schemeDto = schemeService.getSchemeDtoById(schemeId);
            SchemeStatisticResponseDto statistics = schemeService.getSchemeStatistics(schemeId);

            model.addAttribute("scheme", schemeDto);
            model.addAttribute("statistics", statistics);

            return "scheme-details";

        } catch (IllegalArgumentException e) {
            return "redirect:/schemes/all";
        }
    }

    private WeekdaysKeeper convertToWeekdaysKeeper(String[] days) {
        boolean monday = false, tuesday = false, wednesday = false;
        boolean thursday = false, friday = false, saturday = false, sunday = false;

        if (days == null || days.length == 0) {
            return new WeekdaysKeeper(
                    false, false, false,
                    false, false, false, false);
        }

        for (String day : days) {
            if (day == null || day.trim().isEmpty()) continue;

            String upperDay = day.trim().toUpperCase();
            if (upperDay.equals("MONDAY")) monday = true;
            else if (upperDay.equals("TUESDAY")) tuesday = true;
            else if (upperDay.equals("WEDNESDAY")) wednesday = true;
            else if (upperDay.equals("THURSDAY")) thursday = true;
            else if (upperDay.equals("FRIDAY")) friday = true;
            else if (upperDay.equals("SATURDAY")) saturday = true;
            else if (upperDay.equals("SUNDAY")) sunday = true;
        }

        return new WeekdaysKeeper(monday, tuesday, wednesday, thursday, friday, saturday, sunday);
    }
}
