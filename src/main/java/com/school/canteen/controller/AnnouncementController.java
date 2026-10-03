package com.school.canteen.controller;

import com.school.canteen.dto.ActionDtos.AnnouncementRequest;
import com.school.canteen.entity.ParentAnnouncement;
import com.school.canteen.enums.AnnouncementStatus;
import com.school.canteen.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "家长公告")
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @Operation(summary = "公告列表")
    @GetMapping
    public List<ParentAnnouncement> list(@RequestParam(required = false) AnnouncementStatus status) {
        return announcementService.list(status);
    }

    @Operation(summary = "创建家长公告（可关联异常与受影响餐次）")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CANTEEN','PROCUREMENT')")
    public ParentAnnouncement create(@Valid @RequestBody AnnouncementRequest req, Principal principal) {
        return announcementService.create(req, principal.getName());
    }

    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','CANTEEN')")
    public ParentAnnouncement publish(@PathVariable Long id) {
        return announcementService.publish(id);
    }
}
