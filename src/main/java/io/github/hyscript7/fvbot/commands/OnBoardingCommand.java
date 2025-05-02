package io.github.hyscript7.fvbot.commands;

import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.ICommand;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import io.github.hyscript7.fvbot.data.models.User;
import io.github.hyscript7.fvbot.events.OnBoardingEventHandler;
import io.github.hyscript7.fvbot.services.FvBotConfigurationService;
import io.github.hyscript7.fvbot.services.UserService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

@Component
@Slf4j
public class OnBoardingCommand implements ICommand {

    private final IEmbedProvider defaultEmbedProvider;

    private final FvBotConfigurationService fvBotConfigurationService;

    private final OnBoardingEventHandler onMemberJoinListener;

    private final UserService userService;
    private static final String NAME = "onboarding";
    private static final String DESCRIPTION = "Allows you to restart onboarding for a member.";

    private static final String USER_OPTION_ARG_NAME = "user";
    private static final String USER_OPTION_ARG_DESCRIPTION = "User to restart onboarding for.";

    OnBoardingCommand(UserService userService, OnBoardingEventHandler onMemberJoinListener,
            FvBotConfigurationService fvBotConfigurationService, IEmbedProvider defaultEmbedProvider) {
        this.userService = userService;
        this.onMemberJoinListener = onMemberJoinListener;
        this.fvBotConfigurationService = fvBotConfigurationService;
        this.defaultEmbedProvider = defaultEmbedProvider;
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
        return Commands.slash(getName(), getDescription()).addOption(OptionType.USER, USER_OPTION_ARG_NAME,
                USER_OPTION_ARG_DESCRIPTION, true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) throws CommandException {
        if (event.getGuild() == null) {
            sendError(event, "You must be in a guild to use this command.", defaultEmbedProvider);
            return;
        }
        net.dv8tion.jda.api.entities.User jdaUser = event.getOption(USER_OPTION_ARG_NAME).getAsUser();
        Member member = event.getGuild().getMember(jdaUser);
        if (member == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        userService.setCurrentCharacter(user, null);
        userService.updateUser(user);
        Category category = event.getGuild().getCategoryById(fvBotConfigurationService.getCategoryId());
        if (category == null) {
            log.error("Category with id {} not found! Cannot create chambers realm channel. (Restart Command)",
                    fvBotConfigurationService.getCategoryId());
            sendError(event, "Chambers Realm category not found.\nDoes the bot have access?\n<#"
                    + fvBotConfigurationService.getCategoryId() + ">", defaultEmbedProvider);
            return;
        }
        category.createTextChannel(onMemberJoinListener.getChambersChannelName(member.getUser().getName()))
                .onSuccess(channel -> onMemberJoinListener.initializeOnBoarding(event.getMember(), channel))
                .queue();
        event.getHook().editOriginal("Onboarding restarted.").queue();
    }
}
