package com.smartcollab.local.controller;

import com.smartcollab.local.entity.Team;
import com.smartcollab.local.entity.User;
import com.smartcollab.local.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 역할만 이 API를 호출할 수 있음
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/teams")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(adminService.getAllTeams());
    }
}
