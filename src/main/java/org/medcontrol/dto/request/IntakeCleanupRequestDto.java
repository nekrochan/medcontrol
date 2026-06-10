package org.medcontrol.dto.request;

import java.util.List;

public class IntakeCleanupRequestDto {
    private String schemeId;
    private List<String> newAlarmTimes;

    public String getSchemeId() { return schemeId; }
    public void setSchemeId(String schemeId) { this.schemeId = schemeId; }

    public List<String> getNewAlarmTimes() { return newAlarmTimes; }
    public void setNewAlarmTimes(List<String> newAlarmTimes) { this.newAlarmTimes = newAlarmTimes; }
}
