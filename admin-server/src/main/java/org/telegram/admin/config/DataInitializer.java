package org.telegram.admin.config;

import org.telegram.admin.model.*;
import org.telegram.admin.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Value("${admin.default-admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${admin.default-admin.password:admin123}")
    private String defaultAdminPassword;

    @Value("${admin.default-admin.email:admin@telegram.local}")
    private String defaultAdminEmail;

    @Bean
    public CommandLineRunner initData(AdminRepository adminRepository,
                                       AppUserRepository userRepository,
                                       ChannelRepository channelRepository,
                                       ChatGroupRepository groupRepository,
                                       AnnouncementRepository announcementRepository,
                                       SystemConfigRepository configRepository,
                                       ModerationReportRepository reportRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default admin if not exists
            if (!adminRepository.existsByUsername(defaultAdminUsername)) {
                Admin admin = new Admin();
                admin.setUsername(defaultAdminUsername);
                admin.setPassword(passwordEncoder.encode(defaultAdminPassword));
                admin.setEmail(defaultAdminEmail);
                admin.setRole("SUPER_ADMIN");
                adminRepository.save(admin);
            }

            // Create sample data if database is empty
            if (userRepository.count() == 0) {
                createSampleUsers(userRepository);
                createSampleChannels(channelRepository);
                createSampleGroups(groupRepository);
                createSampleAnnouncements(announcementRepository);
                createSampleConfigs(configRepository);
                createSampleReports(reportRepository);
            }
        };
    }

    private void createSampleUsers(AppUserRepository repo) {
        String[][] users = {
            {"100001", "+8613800001111", "张", "伟", "zhangwei", "ACTIVE"},
            {"100002", "+8613800002222", "李", "娜", "lina", "ACTIVE"},
            {"100003", "+8613800003333", "王", "强", "wangqiang", "ACTIVE"},
            {"100004", "+8613800004444", "赵", "敏", "zhaomin", "BANNED"},
            {"100005", "+8613800005555", "陈", "辉", "chenhui", "ACTIVE"},
            {"100006", "+8613800006666", "刘", "洋", "liuyang", "ACTIVE"},
            {"100007", "+8613800007777", "周", "杰", "zhoujie", "RESTRICTED"},
            {"100008", "+8613800008888", "吴", "芳", "wufang", "ACTIVE"},
            {"100009", "+8613800009999", "孙", "磊", "sunlei", "ACTIVE"},
            {"100010", "+8613800010000", "朱", "丽", "zhuli", "ACTIVE"},
        };
        for (String[] u : users) {
            AppUser user = new AppUser();
            user.setTelegramId(Long.parseLong(u[0]));
            user.setPhoneNumber(u[1]);
            user.setFirstName(u[2]);
            user.setLastName(u[3]);
            user.setUsername(u[4]);
            user.setStatus(u[5]);
            user.setRegisteredAt(LocalDateTime.now().minusDays((long)(Math.random() * 90)));
            user.setLastActiveAt(LocalDateTime.now().minusHours((long)(Math.random() * 48)));
            user.setMessagesCount((long)(Math.random() * 5000));
            user.setGroupsCount((int)(Math.random() * 20));
            user.setChannelsCount((int)(Math.random() * 10));
            user.setDeviceInfo("Android " + (10 + (int)(Math.random() * 5)));
            user.setAppVersion("12.5.1");
            if ("BANNED".equals(u[5])) {
                user.setBanReason("Spam behavior detected");
            }
            repo.save(user);
        }
    }

    private void createSampleChannels(ChannelRepository repo) {
        String[][] channels = {
            {"1001", "Tech News Daily", "Latest technology news and updates", "technews", "true"},
            {"1002", "Crypto Trading Hub", "Cryptocurrency trading signals and analysis", "cryptohub", "true"},
            {"1003", "Music Lovers", "Share and discover great music", "musiclovers", "true"},
            {"1004", "Gaming Zone", "Game reviews, tips and discussions", "gamingzone", "false"},
            {"1005", "Travel Adventures", "Share travel photos and stories", "traveladv", "true"},
        };
        for (String[] c : channels) {
            Channel channel = new Channel();
            channel.setChannelId(Long.parseLong(c[0]));
            channel.setTitle(c[1]);
            channel.setDescription(c[2]);
            channel.setUsername(c[3]);
            channel.setPublic(Boolean.parseBoolean(c[4]));
            channel.setStatus("ACTIVE");
            channel.setMemberCount((int)(Math.random() * 10000) + 100);
            channel.setMessagesCount((long)(Math.random() * 50000));
            channel.setCreatorId(100001L);
            repo.save(channel);
        }
    }

    private void createSampleGroups(ChatGroupRepository repo) {
        String[][] groups = {
            {"2001", "Java Developers", "Java programming discussion group", "SUPERGROUP"},
            {"2002", "Android Dev Community", "Android development help and discussion", "SUPERGROUP"},
            {"2003", "Design Team", "UI/UX design collaboration", "BASIC"},
            {"2004", "Project Alpha", "Project coordination group", "BASIC"},
            {"2005", "Cloud Computing", "Cloud architecture and DevOps", "SUPERGROUP"},
        };
        for (String[] g : groups) {
            ChatGroup group = new ChatGroup();
            group.setGroupId(Long.parseLong(g[0]));
            group.setTitle(g[1]);
            group.setDescription(g[2]);
            group.setGroupType(g[3]);
            group.setStatus("ACTIVE");
            group.setMemberCount((int)(Math.random() * 500) + 10);
            group.setMessagesCount((long)(Math.random() * 20000));
            group.setCreatorId(100001L);
            repo.save(group);
        }
    }

    private void createSampleAnnouncements(AnnouncementRepository repo) {
        Announcement a1 = new Announcement();
        a1.setTitle("System Maintenance Notice");
        a1.setContent("The system will undergo maintenance from 2:00 AM to 4:00 AM UTC. Services may be temporarily unavailable.");
        a1.setType("MAINTENANCE");
        a1.setPriority("HIGH");
        a1.setTargetAudience("ALL");
        a1.setActive(true);
        a1.setCreatedBy("admin");
        a1.setPublishedAt(LocalDateTime.now());
        repo.save(a1);

        Announcement a2 = new Announcement();
        a2.setTitle("New Features Released - v12.5.1");
        a2.setContent("We are excited to announce new features including improved group calls, story reactions, and enhanced privacy settings.");
        a2.setType("UPDATE");
        a2.setPriority("NORMAL");
        a2.setTargetAudience("ALL");
        a2.setActive(true);
        a2.setCreatedBy("admin");
        a2.setPublishedAt(LocalDateTime.now().minusDays(3));
        repo.save(a2);
    }

    private void createSampleConfigs(SystemConfigRepository repo) {
        Object[][] configs = {
            {"max_message_length", "4096", "NUMBER", "Maximum message character length", "MESSAGING"},
            {"max_file_size_mb", "2048", "NUMBER", "Maximum file upload size in MB", "STORAGE"},
            {"max_group_members", "200000", "NUMBER", "Maximum members per supergroup", "MESSAGING"},
            {"allow_registration", "true", "BOOLEAN", "Allow new user registration", "GENERAL"},
            {"maintenance_mode", "false", "BOOLEAN", "Enable maintenance mode", "GENERAL"},
            {"min_app_version", "12.0.0", "STRING", "Minimum supported app version", "CLIENT"},
            {"rate_limit_messages", "30", "NUMBER", "Max messages per minute per user", "SECURITY"},
            {"enable_2fa", "true", "BOOLEAN", "Enable two-factor authentication", "SECURITY"},
            {"default_language", "zh", "STRING", "Default language code", "GENERAL"},
            {"push_notification_enabled", "true", "BOOLEAN", "Enable push notifications", "NOTIFICATION"},
        };
        for (Object[] c : configs) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey((String)c[0]);
            config.setConfigValue((String)c[1]);
            config.setConfigType((String)c[2]);
            config.setDescription((String)c[3]);
            config.setCategory((String)c[4]);
            config.setUpdatedBy("system");
            repo.save(config);
        }
    }

    private void createSampleReports(ModerationReportRepository repo) {
        ModerationReport r1 = new ModerationReport();
        r1.setReporterId(100001L);
        r1.setReporterName("zhangwei");
        r1.setReportedUserId(100004L);
        r1.setReportedUserName("zhaomin");
        r1.setReportType("SPAM");
        r1.setContentType("MESSAGE");
        r1.setDescription("User is sending spam messages repeatedly in multiple groups");
        r1.setStatus("PENDING");
        repo.save(r1);

        ModerationReport r2 = new ModerationReport();
        r2.setReporterId(100002L);
        r2.setReporterName("lina");
        r2.setReportedUserId(100007L);
        r2.setReportedUserName("zhoujie");
        r2.setReportType("ABUSE");
        r2.setContentType("USER");
        r2.setDescription("User is sending harassing messages to other members");
        r2.setStatus("PENDING");
        repo.save(r2);

        ModerationReport r3 = new ModerationReport();
        r3.setReporterId(100005L);
        r3.setReporterName("chenhui");
        r3.setReportedUserId(100004L);
        r3.setReportedUserName("zhaomin");
        r3.setReportType("FRAUD");
        r3.setContentType("MESSAGE");
        r3.setDescription("User is promoting fraudulent investment schemes");
        r3.setStatus("REVIEWING");
        repo.save(r3);
    }
}
