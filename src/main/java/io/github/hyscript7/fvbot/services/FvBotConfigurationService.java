package io.github.hyscript7.fvbot.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FvBotConfigurationService {
    @Value("${fvbot.guildId}")
    private long guildId;

    @Value("${fvbot.categoryId}")
    private long categoryId;

    @Value("${fvbot.isolatedRoleId}")
    private long isolatedRoleId;

    @Value("${fvbot.memberRoleId}")
    private long memberRoleId;

    public long getGuildId() {
        return guildId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public long getIsolatedRoleId() {
        return isolatedRoleId;
    }

    public long getMemberRoleId() {
        return memberRoleId;
    }
    
}
