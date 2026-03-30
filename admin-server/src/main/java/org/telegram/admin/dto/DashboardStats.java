package org.telegram.admin.dto;

public class DashboardStats {
    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;
    private long newUsersToday;
    private long premiumUsers;
    private long botUsers;
    private long totalChannels;
    private long activeChannels;
    private long totalGroups;
    private long activeGroups;
    private long pendingReports;
    private long activeAnnouncements;
    private long todayAuditActions;
    private long totalMessages;

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
    public long getBannedUsers() { return bannedUsers; }
    public void setBannedUsers(long bannedUsers) { this.bannedUsers = bannedUsers; }
    public long getNewUsersToday() { return newUsersToday; }
    public void setNewUsersToday(long newUsersToday) { this.newUsersToday = newUsersToday; }
    public long getPremiumUsers() { return premiumUsers; }
    public void setPremiumUsers(long premiumUsers) { this.premiumUsers = premiumUsers; }
    public long getBotUsers() { return botUsers; }
    public void setBotUsers(long botUsers) { this.botUsers = botUsers; }
    public long getTotalChannels() { return totalChannels; }
    public void setTotalChannels(long totalChannels) { this.totalChannels = totalChannels; }
    public long getActiveChannels() { return activeChannels; }
    public void setActiveChannels(long activeChannels) { this.activeChannels = activeChannels; }
    public long getTotalGroups() { return totalGroups; }
    public void setTotalGroups(long totalGroups) { this.totalGroups = totalGroups; }
    public long getActiveGroups() { return activeGroups; }
    public void setActiveGroups(long activeGroups) { this.activeGroups = activeGroups; }
    public long getPendingReports() { return pendingReports; }
    public void setPendingReports(long pendingReports) { this.pendingReports = pendingReports; }
    public long getActiveAnnouncements() { return activeAnnouncements; }
    public void setActiveAnnouncements(long activeAnnouncements) { this.activeAnnouncements = activeAnnouncements; }
    public long getTodayAuditActions() { return todayAuditActions; }
    public void setTodayAuditActions(long todayAuditActions) { this.todayAuditActions = todayAuditActions; }
    public long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }
}
