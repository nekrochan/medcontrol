package org.medcontrol.service;

import org.medcontrol.dto.request.CreateSchemeDto;
import org.medcontrol.dto.request.SchemeDateTimeRequestDto;
import org.medcontrol.dto.request.SchemeSimpleDataRequestDto;
import org.medcontrol.dto.request.SchemeStatusRequestDto;
import org.medcontrol.dto.response.SchemeResponseDto;
import org.medcontrol.dto.response.SchemeStatisticResponseDto;
import org.medcontrol.entity.Scheme;

import java.util.List;

public interface SchemeService {
    Scheme createScheme(CreateSchemeDto dto);

    SchemeResponseDto getSchemeDtoById(String schemeId);

    List<SchemeResponseDto> getSchemesByProfileId(String profileId);

    void updateSchemeSimpleData(SchemeSimpleDataRequestDto dto);

    void updateSchemeDateTime(SchemeDateTimeRequestDto dto);

    void updateSchemeStatus(String schemeId, SchemeStatusRequestDto dto);

    void deleteScheme(String schemeId);

    Scheme getSchemeById(String schemeId);

    SchemeStatisticResponseDto getSchemeStatistics(String schemeId);
}
