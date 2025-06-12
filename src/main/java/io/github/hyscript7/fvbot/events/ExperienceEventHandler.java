package io.github.hyscript7.fvbot.events;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.services.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@Slf4j
public class ExperienceEventHandler extends ListenerAdapter {

    private final LevelRoleService levelRoleService;

    private final LevelUpService levelUpService;

    private final CharacterService characterService;

    private final UserService userService;

    private final FvBotConfigurationService fvBotConfigurationService;

    private final ConcurrentMap<Long, Date> voiceChannelMap;
    private final ConcurrentMap<Long, Date> lastMessageMap;

    private static final long ONE_MINUTE = 60 * 1000l;
    private static final long TEN_MINUTES = 10 * 60 * 1000l;
    private static final long THIRTY_MINUTES = 30 * 60 * 1000l;
    private static final long ONE_HOUR = 60 * 60 * 1000l;
    private static final long TWO_HOURS = 2 * 60 * 60 * 1000l;
    private static final long ONE_DAY = 24 * 60 * 60 * 1000l;

    private static final double MULTIPLIER_LESS_THAN_ONE_MINUTE = 2.5;
    private static final double MULTIPLIER_LESS_THAN_TEN_MINUTES = 2;
    private static final double MULTIPLIER_LESS_THAN_THIRTY_MINUTES = 1.75;
    private static final double MULTIPLIER_LESS_THAN_ONE_HOUR = 1.5;
    private static final double MULTIPLIER_LESS_THAN_TWO_HOURS = 1.25;
    private static final double MULTIPLIER_LESS_THAN_ONE_DAY = 1;
    private static final double MULTIPLIER_MORE_THAN_ONE_DAY = 0.75;

    ExperienceEventHandler(FvBotConfigurationService fvBotConfigurationService, UserService userService,
            CharacterService characterService, LevelUpService levelUpService, LevelRoleService levelRoleService) {
        this.fvBotConfigurationService = fvBotConfigurationService;
        this.userService = userService;
        this.characterService = characterService;
        this.voiceChannelMap = new ConcurrentHashMap<>();
        this.lastMessageMap = new ConcurrentHashMap<>();
        this.levelUpService = levelUpService;
        this.levelRoleService = levelRoleService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getGuild().getIdLong() != fvBotConfigurationService.getGuildId()) {
            return;
        }
        if (event.getAuthor().isBot()) {
            return;
        }
        Character character = userService.getSelectedCharacter(userService.getOrCreateUser(event.getAuthor()))
                .orElse(null);
        if (character != null) {
            Date lastMessageDate = lastMessageMap.get(event.getAuthor().getIdLong());
            double messageExperience = calculateExperienceForMessage(event.getMessage().getContentStripped().length(),
                    lastMessageDate);
            lastMessageMap.put(event.getAuthor().getIdLong(), new Date());
            character.setExperience(character.getExperience() + messageExperience);
            runNecessaryUpdates(event.getMember(), character);
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getOldValue() != null && event.getNewValue() == null) {
            Date joinDate = voiceChannelMap.get(event.getMember().getIdLong());
            if (joinDate != null) {
                voiceChannelMap.remove(event.getMember().getIdLong());
                Character character = userService
                        .getSelectedCharacter(userService.getOrCreateUser(event.getMember().getUser())).orElse(null);
                if (character != null) {
                    character.setExperience(
                            character.getExperience() + calculateExperienceForVoiceChat(joinDate, new Date()));
                    runNecessaryUpdates(event.getMember(), character);
                }
                voiceChannelMap.remove(event.getMember().getIdLong());
            }
        } else if (event.getOldValue() == null && event.getNewValue() != null) {
            voiceChannelMap.put(event.getMember().getIdLong(), new Date());
        }
    }

    private double calculateExperienceForVoiceChat(Date joinDate, Date leaveDate) {
        return ((leaveDate.getTime() - joinDate.getTime()) / 1000d) / 10d; // We're dividing by 10.0d meaning 10 seconds
                                                                           // in VC = 1 ExP... I think.
    }

    private double calculateExperienceForMessage(int messageLength, Date lastMessageDate) {
        double messageExperience = messageLength;
        if (lastMessageDate != null) {
            long timeDiff = new Date().getTime() - lastMessageDate.getTime();
            if (timeDiff < ONE_MINUTE) {
                messageExperience *= MULTIPLIER_LESS_THAN_ONE_MINUTE;
            } else if (timeDiff < TEN_MINUTES) {
                messageExperience *= MULTIPLIER_LESS_THAN_TEN_MINUTES;
            } else if (timeDiff < THIRTY_MINUTES) {
                messageExperience *= MULTIPLIER_LESS_THAN_THIRTY_MINUTES;
            } else if (timeDiff < ONE_HOUR) {
                messageExperience *= MULTIPLIER_LESS_THAN_ONE_HOUR;
            } else if (timeDiff < TWO_HOURS) {
                messageExperience *= MULTIPLIER_LESS_THAN_TWO_HOURS;
            } else if (timeDiff < ONE_DAY) {
                messageExperience *= MULTIPLIER_LESS_THAN_ONE_DAY;
            } else {
                messageExperience *= MULTIPLIER_MORE_THAN_ONE_DAY;
            }
        }
        return messageExperience;
    }

    private void runNecessaryUpdates(Member member, Character character) {
        int levelBeforeUpdate = character.getLevel();
        levelUpService.performEligibleLevelUps(character);
        characterService.updateCharacter(character);
        if (levelBeforeUpdate != character.getLevel()) {
            log.info("Character {} has leveled up to level {}!", character.getFullNameWithTitle(),
                    character.getLevel());
            // Update roles
            List<Role> levelRoles = levelRoleService.getLevelRoles(member); // The roles the user is supposed to have
            List<Role> allLevelRoles = levelRoleService.getAllLevelRoles(member.getGuild()); // All level roles
            List<Role> newMemberRoles = member.getRoles().stream()
                    .filter(r -> !allLevelRoles.contains(r) || (allLevelRoles.contains(r) && levelRoles.contains(r)))
                    .toList(); // The member's new (complete) role list
            member.getGuild().modifyMemberRoles(member, newMemberRoles).queue();
        }
    }

}
