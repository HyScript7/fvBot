package io.github.hyscript7.fvbot.events;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.data.models.User;
import io.github.hyscript7.fvbot.services.CharacterService;
import io.github.hyscript7.fvbot.services.FvBotConfigurationService;
import io.github.hyscript7.fvbot.services.UserService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

@Component
@Slf4j
public class OnMemberJoinListener extends ListenerAdapter {

    private final CharacterService characterService;

    private final UserService userService;

    private final FvBotConfigurationService fvBotConfigurationService;
    private static final String CHAMBERS_CHANNEL_PREFIX = "chamber-";
    private static final String ONBOARDING_BUTTON_PREFIX = "onboarding-";
    private static final String CHARACTER_SELECT_BUTTON_PREFIX = "character-select-";
    private static final String CHARACTER_CREATE_BUTTON_PREFIX = "character-create-";

    public OnMemberJoinListener(FvBotConfigurationService fvBotConfigurationService, UserService userService,
            CharacterService characterService) {
        this.fvBotConfigurationService = fvBotConfigurationService;
        this.userService = userService;
        this.characterService = characterService;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != fvBotConfigurationService.getGuildId()) {
            return;
        }

        Category category = event.getGuild().getCategoryById(fvBotConfigurationService.getCategoryId());
        if (category == null) {
            log.error("Category with id {} not found! Cannot create chambers realm channel.",
                    fvBotConfigurationService.getCategoryId());
            return;
        }
        String channelName = getChambersChannelName(event.getMember().getUser().getName());
        // ! This can technically throw a whole range of exceptions, but so can any
        // ! other API interaction, so we just tank it. 👍
        category.createTextChannel(channelName).onSuccess(channel -> initializeOnBoarding(event.getMember(), channel))
                .queue();
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getGuild().getIdLong() != fvBotConfigurationService.getGuildId()) {
            return;
        }

        Category category = event.getGuild().getCategoryById(fvBotConfigurationService.getCategoryId());
        if (category == null) {
            log.error("Category with id {} not found! Cannot create chambers realm channel.",
                    fvBotConfigurationService.getCategoryId());
            return;
        }

        // Unset the user's current character, so that they can re-enter the onboarding
        // if they rejoin.
        userService.setCurrentCharacter(userService.getOrCreateUser(event.getUser().getIdLong()), null);

        String channelName = getChambersChannelName(event.getUser().getName());

        // Clean up any relevant chambers realm channels on member leave
        category.getTextChannels().stream().filter(channel -> channel.getName().equals(channelName))
                .forEach(channel -> {
                    channel.delete().reason("Member left the server.").queue();
                    log.info("Cleaned up chambers realm channel {} due to member leaving the server.", channel.getName());
                });
    }

    /**
     * This method is public so that it can be artificially triggered through admin
     * slash
     * commands. It sends a message to the given channel with a button that triggers
     * the
     * onboarding process when clicked.
     * 
     * @implNote Clean up is handled in
     *           {@link OnMemberRemoveListener#onGuildMemberRemove(GuildMemberRemoveEvent)},
     *           which only looks for channels in the Chambers category.
     *           If you intend for the used channel to be cleaned up when a member
     *           leaves,
     *           make sure to use the Chambers category, which's ID is returned by
     *           the {@link FvBotConfigurationService}.
     *
     * @param discordUser the user for whom the onboarding process should be
     *                    triggered.
     * @param channel     the channel in which the message should be sent.
     */
    public void initializeOnBoarding(Member discordUser, TextChannel channel) {
        // This method is public so that it can be artificially triggered through admin
        // slash
        // commands.
        Button button = Button.success(buttonIdFor(ONBOARDING_BUTTON_PREFIX, discordUser.getIdLong()), "Begin");
        String messageContent = "Welcome to the chambers realm, " + discordUser.getUser().getName()
                + "!\nClick the button below to begin your onboarding.";
        channel.sendMessage(messageContent).addActionRow(button).queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getGuild() == null) {
            // This event listener only cares about buttons in guilds, since that's where
            // the onboarding happens
            return;
        }
        Message message = event.getMessage();
        Member member = event.getMember();
        if (member == null) {
            // Legit how did we get a button interaction without a member attached to it?
            // This shi ain't even in a DM channel.
            return;
        }
        User user = userService.getOrCreateUser(member.getIdLong());
        // We need to split by the last - to get the button id
        // e.g. ONBOARDING_BUTTON_PREFIX + getLongAsHex(member.getIdLong())
        // Since we can get the user id hex, we can split by that
        String userIdHex = getLongAsHex(member.getIdLong());
        String buttonId = event.getComponentId().split(userIdHex)[0];
        switch (buttonId) {
            case ONBOARDING_BUTTON_PREFIX -> handleInitializeOnBoardingButton(event, message, member, user);
            case CHARACTER_SELECT_BUTTON_PREFIX -> handleExistingCharacterButton(event, message, member, user);
            case CHARACTER_CREATE_BUTTON_PREFIX -> handleCreateCharacterButton(event, message, member, user);
        }
    }

    private void handleInitializeOnBoardingButton(ButtonInteractionEvent event, Message message, Member member,
            User user) {
        String messageContent;
        List<ItemComponent> components = new ArrayList<>(2);
        components.add(Button.success(buttonIdFor(CHARACTER_CREATE_BUTTON_PREFIX, member.getIdLong()),
                "Create New Character"));
        // If the user has characters, we also add a character select button.
        if (userService.hasCharacters(user)) {
            messageContent = "Before you may access the server, you must choose or create a character.";
            components.add(Button.primary(buttonIdFor(CHARACTER_SELECT_BUTTON_PREFIX, member.getIdLong()),
                    "Select Existing Character"));
        } else {
            messageContent = "Before you may access the server, you must create a character.";
        }
        event.editMessage(messageContent).setComponents(ActionRow.of(components)).queue();
    }

    private void handleCreateCharacterButton(ButtonInteractionEvent event, Message message, Member member, User user) {
        event.editMessage("Character creation not yet implemented.").setComponents().queue();
    }

    private void handleExistingCharacterButton(ButtonInteractionEvent event, Message message, Member member,
            User user) {
        event.editMessage("Character selection not yet implemented.").setComponents().queue();
    }

    private String getLongAsHex(long id) {
        return Long.toHexString(id);
    }

    private String buttonIdFor(String prefix, long userId) {
        return prefix + getLongAsHex(userId);
    }

    /**
     * Gets a channel name for the chambers realm channel for a given user.
     * The channel name is in the format "chambers-<username>".
     * The maximum length of the returned channel name is 100 characters.
     * 
     * @param username The username of the user.
     * @return A channel name for the chambers realm channel.
     */
    private String getChambersChannelName(String username) {
        String channelName = CHAMBERS_CHANNEL_PREFIX + username;
        // We need to clamp channelName to a max of 100 characters by removing
        // characters from the end. This limit is set by Discord.
        if (channelName.length() > 100) {
            channelName = channelName.substring(0, 100);
        }
        return channelName;
    }

}
