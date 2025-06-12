package io.github.hyscript7.fvbot.commands;

import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.ICommand;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.User;
import io.github.hyscript7.fvbot.services.LevelRoleService;
import io.github.hyscript7.fvbot.services.UserService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

@Component
@Slf4j
public class LevelCommand implements ICommand {

    private final LevelRoleService levelRoleService;

    private final IEmbedProvider defaultEmbedProvider;

    private final UserService userService;

    private static final String NAME = "level";
    private static final String DESCRIPTION = "Provides commands for working with the level system.";

    private static final String MEMBER_OPTION_ARG_NAME = "member";
    private static final String MEMBER_OPTION_ARG_DESCRIPTION = "The member who's level is of concern";

    LevelCommand(UserService userService, IEmbedProvider defaultEmbedProvider, LevelRoleService levelRoleService) {
        this.userService = userService;
        this.defaultEmbedProvider = defaultEmbedProvider;
        this.levelRoleService = levelRoleService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public SlashCommandData getSlashCommandData() {
        return Commands.slash(getName(), getDescription()).addOption(OptionType.USER, MEMBER_OPTION_ARG_NAME,
                MEMBER_OPTION_ARG_DESCRIPTION);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) throws CommandException {
        @Nullable
        OptionMapping opt = event.getOption(MEMBER_OPTION_ARG_NAME);
        Member member = null;
        if (opt != null) {
            member = opt.getAsMember();
        }
        if (member == null) {
            member = event.getMember();
        }
        User user = userService.getOrCreateUser(member.getUser());
        Optional<Character> character = Optional.ofNullable(user.getCurrentCharacter());
        if (character.isEmpty()) {
            sendError(event, "You do not have a character equipped!", defaultEmbedProvider);
            return;
        }
        String fullname = character.get().getFullNameWithTitle();
        int level = character.get().getLevel();
        int prestige = character.get().getResurrection();
        sendPretty(event,
                fullname + " is currently level " + level + " 🌟 " + prestige + "\nYour roles: " + levelRoleService
                        .getLevelRoles(member).stream().map(Role::getName).collect(Collectors.joining(", ")),
                defaultEmbedProvider);
    }
}
