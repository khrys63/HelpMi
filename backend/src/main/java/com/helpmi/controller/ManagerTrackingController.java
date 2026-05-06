package com.helpmi.controller;

import com.helpmi.dto.response.ManagerTrackingResponse;
import com.helpmi.service.ManagerTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/managers")
@RequiredArgsConstructor
public class ManagerTrackingController {

    private final ManagerTrackingService managerTrackingService;

    @GetMapping
    public ManagerTrackingResponse getManagerTracking() {
        return managerTrackingService.getManagerTracking();
    }
}
