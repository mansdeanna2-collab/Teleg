package org.telegram.admin.service;

import org.telegram.admin.model.Channel;
import org.telegram.admin.repository.ChannelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final AuditLogService auditLogService;

    public ChannelService(ChannelRepository channelRepository, AuditLogService auditLogService) {
        this.channelRepository = channelRepository;
        this.auditLogService = auditLogService;
    }

    public Page<Channel> searchChannels(String keyword, String status, Pageable pageable) {
        return channelRepository.searchChannels(keyword, status, pageable);
    }

    public Channel getById(Long id) {
        return channelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
    }

    public Channel create(Channel channel) {
        return channelRepository.save(channel);
    }

    public Channel update(Long id, Channel channel) {
        Channel existing = getById(id);
        existing.setTitle(channel.getTitle());
        existing.setDescription(channel.getDescription());
        existing.setUsername(channel.getUsername());
        existing.setPublic(channel.isPublic());
        existing.setVerified(channel.isVerified());
        return channelRepository.save(existing);
    }

    public Channel suspend(Long id, String adminUsername) {
        Channel channel = getById(id);
        channel.setStatus("SUSPENDED");
        Channel saved = channelRepository.save(channel);
        auditLogService.log(adminUsername, "SUSPEND", "CHANNEL", id.toString(),
                "Suspended channel: " + channel.getTitle(), null);
        return saved;
    }

    public Channel activate(Long id, String adminUsername) {
        Channel channel = getById(id);
        channel.setStatus("ACTIVE");
        Channel saved = channelRepository.save(channel);
        auditLogService.log(adminUsername, "ACTIVATE", "CHANNEL", id.toString(),
                "Activated channel: " + channel.getTitle(), null);
        return saved;
    }

    public void delete(Long id, String adminUsername) {
        Channel channel = getById(id);
        channel.setStatus("DELETED");
        channelRepository.save(channel);
        auditLogService.log(adminUsername, "DELETE", "CHANNEL", id.toString(),
                "Deleted channel: " + channel.getTitle(), null);
    }

    public long countByStatus(String status) {
        return channelRepository.countByStatus(status);
    }
}
