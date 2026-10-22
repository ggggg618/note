package com.note.admin.controller;

import com.note.admin.dto.AiStatsResponse;
import com.note.admin.dto.AuditRequest;
import com.note.admin.dto.NoteAdminResponse;
import com.note.admin.dto.UserAdminResponse;
import com.note.admin.service.AdminService;
import com.note.common.BusinessException;
import com.note.common.PageResult;
import com.note.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public Result<PageResult<UserAdminResponse>> listUsers(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listUsers(page, size, keyword));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> setUserStatus(@PathVariable Long id,
                                      @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            throw new BusinessException("状态不能为空");
        }
        adminService.setUserStatus(id, status);
        return Result.ok();
    }

    @GetMapping("/notes")
    public Result<PageResult<NoteAdminResponse>> listNotes(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer auditStatus) {
        return Result.ok(adminService.listNotes(page, size, auditStatus));
    }

    @PutMapping("/notes/{id}/audit")
    public Result<Void> auditNote(@PathVariable Long id,
                                  @RequestBody AuditRequest request) {
        if (request == null || request.getAuditStatus() == null) {
            throw new BusinessException("审核状态不能为空");
        }
        adminService.auditNote(id, request.getAuditStatus());
        return Result.ok();
    }

    @PutMapping("/notes/{id}/offline")
    public Result<Void> offlineNote(@PathVariable Long id) {
        adminService.offlineNote(id);
        return Result.ok();
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        adminService.deleteComment(id);
        return Result.ok();
    }

    @GetMapping("/ai-stats")
    public Result<AiStatsResponse> aiStats() {
        return Result.ok(adminService.aiStats());
    }
}
