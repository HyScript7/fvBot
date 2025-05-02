package io.github.hyscript7.fvbot.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.Title;
import io.github.hyscript7.fvbot.data.models.User;
import io.github.hyscript7.fvbot.services.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

@Component
@Slf4j
public class OnBoardingEventHandler extends ListenerAdapter {

    private final IEmbedProvider defaultEmbedProvider;

    private final TitleService titleService;

    private final InnateNameService innateNameService;

    private final CharacterService characterService;

    private final UserService userService;

    private final FvBotConfigurationService fvBotConfigurationService;
    private static final String CHAMBERS_CHANNEL_PREFIX = "chamber-";
    private static final String ONBOARDING_BUTTON_COMPONENT_ID = "onboarding";
    private static final String CHARACTER_SELECT_BUTTON_COMPONENT_ID = "character-select";
    private static final String CHARACTER_CREATE_BUTTON_COMPONENT_ID = "character-create";
    private static final String CHARACTER_SELECT_DROPDOWN_COMPONENT_ID = "character-select-dropdown";
    private static final String CHARACTER_SELECT_DROPDOWN_ABORT_VALUE = "abort"; // Since we only otherwise deal with
                                                                                 // hex values in this dropdown's
                                                                                 // values, it's safe to use the word
                                                                                 // 'abort'.
    private static final String FULL_NAME_TEXT_INPUT_COMPONENT_ID = "creation-modal:first-name";
    private static final String CHARACTER_CREATION_MODAL_ID = "character-creation-modal";
    private static final String CHARACTER_CREATION_REROLL_INNATE_NAME_BUTTON_COMPONENT_ID = "character-reroll-innate-name";
    private static final String CHARACTER_CREATION_ACCEPT_INNATE_NAME_BUTTON_COMPONENT_ID = "character-accept-innate-name";
    private static final String TITLE_SELECT_DROPDOWN_COMPONENT_ID = "character-creation-title-select-dropdown";
    private static final String TITLE_SELECT_DROPDOWN_NONE_VALUE = "none";

    private static final int RANDOM_INNATE_NAME_MIN_LENGTH = 4;
    private static final int RANDOM_INNATE_NAME_MAX_LENGTH = 9;

    public OnBoardingEventHandler(FvBotConfigurationService fvBotConfigurationService, UserService userService,
            CharacterService characterService, InnateNameService innateNameService,
            TitleService titleService, IEmbedProvider defaultEmbedProvider) {
        this.fvBotConfigurationService = fvBotConfigurationService;
        this.userService = userService;
        this.characterService = characterService;
        this.innateNameService = innateNameService;
        this.titleService = titleService;
        this.defaultEmbedProvider = defaultEmbedProvider;
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

        String channelName = getChambersChannelName(event.getUser().getName());

        // Clean up any relevant chambers realm channels on member leave
        cleanupChambersChannels(event, channelName);

        // Unset the user's current character, so that they can re-enter the onboarding
        // if they rejoin.
        User user = userService.getOrCreateUser(event.getUser());
        if (userService.getSelectedCharacter(user).isPresent()) {
            userService.setCurrentCharacter(user, null);
        }
    }

    /**
     * Deletes any text channels in the configured category which have a name
     * matching
     * the given matchName.
     * 
     * @see OnBoardingEventHandler#getChambersChannelName(String)
     * @see FvBotConfigurationService#getCategoryId()
     * 
     * @param event     The event that triggered the cleanup. Used to get the guild
     *                  in which
     *                  the cleanup is taking place.
     * @param matchName
     *                  The name of the channels to delete.
     */
    public void cleanupChambersChannels(GenericGuildEvent event, String matchName) {
        Category category = event.getGuild().getCategoryById(fvBotConfigurationService.getCategoryId());
        if (category == null) {
            log.error("Category with id {} not found! Cannot create chambers realm channel.",
                    fvBotConfigurationService.getCategoryId());
            return;
        }
        category.getTextChannels().stream().filter(channel -> channel.getName().equals(matchName))
                .forEach(channel -> {
                    channel.delete().reason("Member left the server.").queue();
                    log.info("Cleaned up chambers realm channel {} ({}) due to member leaving the server.",
                            channel.getName(), channel.getId());
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
        // slash commands.
        Button button = Button.success(ONBOARDING_BUTTON_COMPONENT_ID, "Begin");
        String messageContent = "Welcome to the chambers realm, " + discordUser.getUser().getName()
                + "!\nClick the button below to begin your onboarding.";
        isolateChannel(channel.getGuild(), discordUser, channel);
        grantUserIsolatedRole(discordUser);
        channel.sendMessageEmbeds(getEmbedProvider().getPrettyEmbedBuilder(discordUser.getJDA().getSelfUser())
                .setDescription(messageContent).build()).setComponents(ActionRow.of(button)).queue();
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
        User user = userService.getOrCreateUser(member.getUser());
        switch (event.getComponentId()) {
            case ONBOARDING_BUTTON_COMPONENT_ID -> handleInitializeOnBoardingButton(event, message, member, user);
            case CHARACTER_SELECT_BUTTON_COMPONENT_ID -> handleExistingCharacterButton(event, message, member, user);
            case CHARACTER_CREATE_BUTTON_COMPONENT_ID -> handleCreateCharacterButton(event, message, member, user);
            case CHARACTER_CREATION_REROLL_INNATE_NAME_BUTTON_COMPONENT_ID ->
                handleInnateNameRerollButton(event, message, member, user);
            case CHARACTER_CREATION_ACCEPT_INNATE_NAME_BUTTON_COMPONENT_ID ->
                handleInnateNameAcceptButton(event, message, member, user);
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        // This is the exact same thing as a button interaction, but with a dropdown.
        if (event.getGuild() == null) {
            return;
        }
        Message message = event.getMessage();
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        switch (event.getComponentId()) {
            case CHARACTER_SELECT_DROPDOWN_COMPONENT_ID -> handleCharacterSelectDropdown(event, message, member, user);
            case TITLE_SELECT_DROPDOWN_COMPONENT_ID -> handleTitleSelectionDropdown(event, message, member, user);
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        // This is the exact same thing as a button interaction, but with a dropdown.
        if (event.getGuild() == null) {
            return;
        }
        Message message = event.getMessage();
        Member member = event.getMember();
        if (member == null) {
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        switch (event.getModalId()) {
            case CHARACTER_CREATION_MODAL_ID -> handleCharacterCreationModalSubmission(event, message, member, user);
        }
    }

    private void handleInitializeOnBoardingButton(IMessageEditCallback event, Message message, Member member,
            User user) {
        String messageContent;
        List<ItemComponent> components = new ArrayList<>(2);
        components.add(Button.success(CHARACTER_CREATE_BUTTON_COMPONENT_ID,
                "Create New Character"));
        // If the user has characters, we also add a character select button.
        if (userService.hasCharacters(user)) {
            messageContent = "Before you may access the server, you must choose or create a character.";
            components.add(Button.primary(CHARACTER_SELECT_BUTTON_COMPONENT_ID,
                    "Select Existing Character"));
        } else {
            messageContent = "Before you may access the server, you must create a character.";
        }
        event.editMessageEmbeds(
                getEmbedProvider().getPrettyEmbedBuilder(member.getJDA().getSelfUser()).setDescription(messageContent)
                        .build())
                .setComponents(ActionRow.of(components)).queue();
    }

    private void handleExistingCharacterButton(ButtonInteractionEvent event, Message message, Member member,
            User user) {
        List<Character> characters = characterService.getCharactersOfUser(user);
        // ! SelectMenu supports only up to 25 options. Implement pagination later.
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu
                .create(CHARACTER_SELECT_DROPDOWN_COMPONENT_ID).setRequiredRange(1, 1);
        characters.forEach(character -> selectMenuBuilder.addOption(
                character.getFullName() + " (" + character.getInnateName() + ")", getLongAsHex(character.getId())));
        selectMenuBuilder.addOption("Abort (Return)", CHARACTER_SELECT_DROPDOWN_ABORT_VALUE);
        event.editMessageEmbeds(getEmbedProvider().getPrettyEmbedBuilder(member.getJDA().getSelfUser())
                .setDescription("Select your desired character to proceed.").build())
                .setComponents(ActionRow.of(selectMenuBuilder.build()))
                .queue();
    }

    private void handleCharacterSelectDropdown(StringSelectInteractionEvent event, Message message, Member member,
            User user) {
        String selectedCharacterId = event.getValues().get(0);
        if (selectedCharacterId.equals(CHARACTER_SELECT_DROPDOWN_ABORT_VALUE)) {
            // This is a hack that abuses the fact that the initializeOnboardingButton event
            // only really depends on a messageEdit event.
            // It is for this reason it was written that way.
            handleInitializeOnBoardingButton(event, message, member, user);
            return;
        }
        Optional<Character> character = characterService.getCharacterById(Long.parseLong(selectedCharacterId, 16));
        if (!character.isPresent()) {
            event.editMessageEmbeds(getEmbedProvider().getPrettyEmbedBuilder(member.getJDA().getSelfUser())
                    .setDescription(
                            "Character not found. Please try again.\nMaybe an admin deleted it while you were choosing?")
                    .setColor(0xFF0000).build()).queue();
            return;
        }
        Character selectedCharacter = character.get();
        userService.setCurrentCharacter(user, selectedCharacter);

        userService.setCurrentCharacter(user, selectedCharacter);
        finishOnBoarding(event, message, member, user);
    }

    private void handleCreateCharacterButton(ButtonInteractionEvent event, Message message, Member member, User user) {
        Modal modal = getCharacterCreationModal();
        event.replyModal(modal).queue();
    }

    private void handleCharacterCreationModalSubmission(ModalInteractionEvent event, Message message, Member member,
            User user) {
        if (!event.getModalId().equals(CHARACTER_CREATION_MODAL_ID)) {
            log.error(
                    "Received unexpected modal interaction at handle level: {} (handleCharacterCreationModalSubmission)",
                    event.getModalId());
            return;
        }
        String fullName = event.getValue(FULL_NAME_TEXT_INPUT_COMPONENT_ID).getAsString();
        String firstName = fullName.split(" ")[0];
        String lastName = fullName.split(" ")[1];
        String innateName = innateNameService.generateInnateName(RANDOM_INNATE_NAME_MIN_LENGTH,
                RANDOM_INNATE_NAME_MAX_LENGTH);
        Character character = getDefaultCharacterBuilder().firstName(firstName).lastName(lastName)
                .innateName(innateName).user(user).build();
        characterService.createCharacter(user, character);
        userService.setCurrentCharacter(user, character); // Equip the character so that we can identify it later in
                                                          // other interactions
        sendInnateNameOffer(event, member, innateName);
    }

    private void sendInnateNameOffer(IMessageEditCallback event, Member member, String innateName) {
        Button acceptButton = Button.success(CHARACTER_CREATION_ACCEPT_INNATE_NAME_BUTTON_COMPONENT_ID, "Accept");
        Button rerollButton = Button.danger(CHARACTER_CREATION_REROLL_INNATE_NAME_BUTTON_COMPONENT_ID, "Reroll");
        event.editMessageEmbeds(getEmbedProvider().getPrettyEmbedBuilder(member.getJDA().getSelfUser())
                .setDescription("Now choose an innate name that you like.\nHow does\n```\n" + innateName
                        + "\n```\nsound to you?")
                .build())
                .setComponents(ActionRow.of(acceptButton, rerollButton)).queue();
    }

    private void handleInnateNameRerollButton(ButtonInteractionEvent event, Message message, Member member, User user) {
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (character.isEmpty()) {
            handleInitializeOnBoardingButton(event, message, member, user);
            return;
        }
        String innateName = innateNameService.generateInnateName(RANDOM_INNATE_NAME_MIN_LENGTH,
                RANDOM_INNATE_NAME_MAX_LENGTH);
        characterService.updateInnateName(character.get(), innateName);
        sendInnateNameOffer(event, member, innateName);
    }

    private void handleInnateNameAcceptButton(ButtonInteractionEvent event, Message message, Member member, User user) {
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (character.isEmpty()) {
            handleInitializeOnBoardingButton(event, message, member, user);
            return;
        }
        Character selectedCharacter = character.get();
        userService.setCurrentCharacter(user, selectedCharacter);
        offerTitleOrFinalize(event, message, member, user);
    }

    private void offerTitleOrFinalize(IMessageEditCallback event, Message message, Member member, User user) {
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (character.isEmpty()) {
            handleInitializeOnBoardingButton(event, message, member, user);
            return;
        }
        List<Title> characterBoundTitles = titleService.getCharacterTitles(character.get());
        List<Title> userBoundTitles = titleService.getUserTitles(user);
        List<Title> allTitles = new ArrayList<>(characterBoundTitles);
        allTitles.addAll(userBoundTitles);
        if (allTitles.isEmpty()) {
            log.info("User " + member.getId() + " has no titles. Proceeding to finalization.");
            finishOnBoarding(event, message, member, user);
            return;
        }
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create(TITLE_SELECT_DROPDOWN_COMPONENT_ID)
                .setRequiredRange(1, 1);
        allTitles.forEach(title -> selectMenuBuilder.addOption(getTitlePreview(title, character.get()),
                getLongAsHex(title.getId())));
        selectMenuBuilder.addOption("None", TITLE_SELECT_DROPDOWN_NONE_VALUE);
        event.editMessageEmbeds(getEmbedProvider().getPrettyEmbedBuilder(member.getJDA().getSelfUser())
                .setDescription("Select your desired title to proceed.").build())
                .setComponents(ActionRow.of(selectMenuBuilder.build()))
                .queue();
    }

    private void handleTitleSelectionDropdown(StringSelectInteractionEvent event, Message message, Member member,
            User user) {
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (character.isEmpty()) {
            handleInitializeOnBoardingButton(event, message, member, user);
            return;
        }
        String selectedTitleId = event.getValues().get(0);
        if (selectedTitleId.equals(TITLE_SELECT_DROPDOWN_NONE_VALUE)) {
            finishOnBoarding(event, message, member, user);
            return;
        }
        Optional<Title> title = titleService.getTitleById(Long.parseLong(selectedTitleId, 16));
        if (!title.isPresent()) {
            event.editMessageEmbeds(getEmbedProvider().getPrettyEmbedBuilder(member.getJDA().getSelfUser())
                    .setDescription(
                            "Title not found. Please try again.\nMaybe an admin deleted it while you were choosing?")
                    .setColor(0xFF0000).build())
                    .queue();
            return;
        }
        character.get().setTitle(title.get());
        characterService.updateCharacter(character.get());
        finishOnBoarding(event, message, member, user);
    }

    private void finishOnBoarding(IMessageEditCallback event, Message message, Member member, User user) {
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (character.isEmpty()) {
            handleInitializeOnBoardingButton(event, message, member, user);
            return;
        }
        event.editMessageEmbeds(getEmbedProvider().getPrettyEmbedBuilder(member.getJDA().getSelfUser())
                .setDescription("You're all set up!\nHope you enjoy your stay, "
                        + character.get().getFullNameWithTitle() + "!\n-# This channel will be deleted momentarily.")
                .build()).setComponents().queue();
        grantUserMemberRole(member);
        try {
            member.getGuild().modifyNickname(member, character.get().discordFullName()).queue();
        } catch (HierarchyException e) {
            log.warn("Failed to change nickname of " + member.getId() + " to " + character.get().discordFullName());
        }
        message.getChannel().delete().queueAfter(5, TimeUnit.SECONDS);
    }

    private String getLongAsHex(long id) {
        return Long.toHexString(id);
    }

    private Modal getCharacterCreationModal() {
        TextInput fullName = TextInput.create(FULL_NAME_TEXT_INPUT_COMPONENT_ID, "Full Name", TextInputStyle.SHORT)
                .setRequired(true).build();
        return Modal.create(CHARACTER_CREATION_MODAL_ID, "Character Creation")
                .addComponents(ActionRow.of(fullName)).build();
    }

    private String getTitlePreview(Title title, Character character) {
        String prefix = title.getPrefix() == null ? "" : title.getPrefix();
        String suffix = title.getSuffix() == null ? "" : title.getSuffix();
        return prefix + character.getFirstName().charAt(0) + character.getLastName().charAt(0) + suffix;
    }

    /**
     * Gets a channel name for the chambers realm channel for a given user.
     * The channel name is in the format "chambers-<username>".
     * The maximum length of the returned channel name is 100 characters.
     * 
     * @param username The username of the user.
     * @return A channel name for the chambers realm channel.
     */
    public String getChambersChannelName(String username) {
        String channelName = CHAMBERS_CHANNEL_PREFIX + username;
        // We need to clamp channelName to a max of 100 characters by removing
        // characters from the end. This limit is set by Discord.
        if (channelName.length() > 100) {
            channelName = channelName.substring(0, 100);
        }
        return channelName;
    }

    /**
     * Provides a default character builder initialized with standard starting
     * attributes for a new character.
     *
     * @return a CharacterBuilder with level set to 1, resurrection set to 0,
     *         and experience set to 0.0.
     */
    private Character.CharacterBuilder getDefaultCharacterBuilder() {
        return Character.builder().level(1).resurrection(0).experience(0.0);
    }

    private Role getIsolatedRole(Guild guild) {
        return guild.getRoleById(fvBotConfigurationService.getIsolatedRoleId());
    }

    private Role getMemberRole(Guild guild) {
        return guild.getRoleById(fvBotConfigurationService.getMemberRoleId());
    }

    /**
     * Isolates a given TextChannel by removing the VIEW_CHANNEL and MESSAGE_SEND
     * permissions from the public role and adding the VIEW_CHANNEL permission
     * for the given member. This is used to create a channel that only the
     * member can see.
     *
     * @param guild   The guild that the channel is in.
     * @param member  The member that should be able to see the channel.
     * @param channel The channel to isolate.
     */
    private void isolateChannel(Guild guild, Member member, TextChannel channel) {
        channel.upsertPermissionOverride(guild.getPublicRole())
                .setDenied(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND).queue();
        channel.upsertPermissionOverride(member).setAllowed(Permission.VIEW_CHANNEL).queue();
    }

    /**
     * Grants the isolated role to the given member. This role is used to prevent
     * users from seeing the onboarding channels until they have completed the
     * onboarding process. This method is used to add the isolated role to a user
     * when they join the server.
     *
     * @param member The guild member to whom the isolated role should be granted.
     */
    private void grantUserIsolatedRole(Member member) {
        if (member.getRoles().contains(getMemberRole(member.getGuild()))) {
            member.getGuild().removeRoleFromMember(member, getMemberRole(member.getGuild())).queue();
        }
        member.getGuild().addRoleToMember(member, getIsolatedRole(member.getGuild())).queue();
    }

    /**
     * Grants the member role to the given member by removing the isolated role,
     * if present, and adding the member role.
     *
     * @param member The guild member to whom the member role should be granted.
     */
    private void grantUserMemberRole(Member member) {
        if (member.getRoles().contains(getIsolatedRole(member.getGuild()))) {
            member.getGuild().removeRoleFromMember(member, getIsolatedRole(member.getGuild())).queue();
        }
        member.getGuild().addRoleToMember(member, getMemberRole(member.getGuild())).queue();
    }

    private IEmbedProvider getEmbedProvider() {
        return defaultEmbedProvider;
    }

}
