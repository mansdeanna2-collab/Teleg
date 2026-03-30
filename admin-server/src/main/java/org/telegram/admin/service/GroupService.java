package org.telegram.admin.service;

import org.telegram.admin.model.ChatGroup;
import org.telegram.admin.repository.ChatGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

    private final ChatGroupRepository groupRepository;
    private final AuditLogService auditLogService;

    public GroupService(ChatGroupRepository groupRepository, AuditLogService auditLogService) {
        this.groupRepository = groupRepository;
        this.auditLogService = auditLogService;
    }

    public Page<ChatGroup> searchGroups(String keyword, String status, Pageable pageable) {
        return groupRepository.searchGroups(keyword, status, pageable);
    }

    public ChatGroup getById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    public ChatGroup create(ChatGroup group) {
        return groupRepository.save(group);
    }

    public ChatGroup update(Long id, ChatGroup group) {
        ChatGroup existing = getById(id);
        existing.setTitle(group.getTitle());
        existing.setDescription(group.getDescription());
        existing.setGroupType(group.getGroupType());
        existing.setPublic(group.isPublic());
        return groupRepository.save(existing);
    }

    public ChatGroup suspend(Long id, String adminUsername) {
        ChatGroup group = getById(id);
        group.setStatus("SUSPENDED");
        ChatGroup saved = groupRepository.save(group);
        auditLogService.log(adminUsername, "SUSPEND", "GROUP", id.toString(),
                "Suspended group: " + group.getTitle(), null);
        return saved;
    }

    public ChatGroup activate(Long id, String adminUsername) {
        ChatGroup group = getById(id);
        group.setStatus("ACTIVE");
        ChatGroup saved = groupRepository.save(group);
        auditLogService.log(adminUsername, "ACTIVATE", "GROUP", id.toString(),
                "Activated group: " + group.getTitle(), null);
        return saved;
    }

    public void delete(Long id, String adminUsername) {
        ChatGroup group = getById(id);
        group.setStatus("DELETED");
        groupRepository.save(group);
        auditLogService.log(adminUsername, "DELETE", "GROUP", id.toString(),
                "Deleted group: " + group.getTitle(), null);
    }

    public long countByStatus(String status) {
        return groupRepository.countByStatus(status);
    }
}
