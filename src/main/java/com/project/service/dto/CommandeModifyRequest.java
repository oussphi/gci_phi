package com.project.service.dto;

import java.util.List;

public class CommandeModifyRequest {
    private List<Long> selectedLineIds;

    public List<Long> getSelectedLineIds() {
        return selectedLineIds;
    }

    public void setSelectedLineIds(List<Long> selectedLineIds) {
        this.selectedLineIds = selectedLineIds;
    }
}

