package org.medcontrol.dto.request;

public class SchemeStatusRequestDto {
    private String schemeId;
    private String status;

    public SchemeStatusRequestDto() {}

    public String getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(String schemeId) {
        this.schemeId = schemeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
