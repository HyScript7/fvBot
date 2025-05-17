package io.github.hyscript7.fvbot.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.ICommand;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.Title;
import io.github.hyscript7.fvbot.data.models.User;
import io.github.hyscript7.fvbot.services.CharacterService;
import io.github.hyscript7.fvbot.services.TitleService;
import io.github.hyscript7.fvbot.services.UserService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

@Component
@Slf4j
public class TitleCommand implements ICommand {

    private final IEmbedProvider defaultEmbedProvider;

    private final CharacterService characterService;

    private final UserService userService;

    private final TitleService titleService;
    private static final String NAME = "title";
    private static final String DESCRIPTION = "Provides sub-commands for you to work with titles.";

    private static final String USER_OPTION_ARG_NAME = "user";
    private static final String USER_OPTION_ARG_DESCRIPTION = "User to list or manage titles of.";
    private static final String TITLE_ID_OPTION_ARG_NAME = "id";
    private static final String TITLE_ID_OPTION_ARG_DESCRIPTION = "Id of the title.";
    private static final String TITLE_PREFIX_OPTION_ARG_NAME = "prefix";
    private static final String TITLE_PREFIX_OPTION_ARG_DESCRIPTION = "Prefix of the title.";
    private static final String TITLE_SUFFIX_OPTION_ARG_NAME = "suffix";
    private static final String TITLE_SUFFIX_OPTION_ARG_DESCRIPTION = "Suffix of the title.";
    private static final String TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME = "character-only";
    private static final String TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_DESCRIPTION = "Grant the title to the character only.";

    TitleCommand(TitleService titleService, UserService userService, CharacterService characterService,
            IEmbedProvider defaultEmbedProvider) {
        this.titleService = titleService;
        this.userService = userService;
        this.characterService = characterService;
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
        return Commands.slash(getName(), getDescription()).addSubcommands(
                new SubcommandData("refresh", "Refreshes your nickname."),
                new SubcommandData("equip", "Equips a title.").addOption(OptionType.STRING, TITLE_ID_OPTION_ARG_NAME,
                        TITLE_ID_OPTION_ARG_DESCRIPTION, true),
                new SubcommandData("unequip", "Unequips your current title."),
                new SubcommandData("list", "Lists all of your titles.").addOption(OptionType.USER, USER_OPTION_ARG_NAME,
                        USER_OPTION_ARG_DESCRIPTION, false))
                .addSubcommandGroups(
                        new SubcommandGroupData("admin", "Admin commands for managing titles.").addSubcommands(
                                new SubcommandData("refresh", "Refreshes another members nickname.")
                                        .addOption(OptionType.USER, USER_OPTION_ARG_NAME, USER_OPTION_ARG_DESCRIPTION,
                                                true),
                                new SubcommandData("add", "Adds a title.").addOption(OptionType.STRING,
                                        TITLE_PREFIX_OPTION_ARG_NAME, TITLE_PREFIX_OPTION_ARG_DESCRIPTION, false)
                                        .addOption(OptionType.STRING, TITLE_SUFFIX_OPTION_ARG_NAME,
                                                TITLE_SUFFIX_OPTION_ARG_DESCRIPTION,
                                                false),
                                new SubcommandData("remove", "Removes a title.").addOption(OptionType.STRING,
                                        TITLE_ID_OPTION_ARG_NAME, TITLE_ID_OPTION_ARG_DESCRIPTION, true),
                                new SubcommandData("list", "Lists all of the titles."),
                                new SubcommandData("grant", "Grants a title to a member.").addOption(OptionType.USER,
                                        USER_OPTION_ARG_NAME, USER_OPTION_ARG_DESCRIPTION, true)
                                        .addOption(OptionType.STRING, TITLE_ID_OPTION_ARG_NAME,
                                                TITLE_ID_OPTION_ARG_DESCRIPTION, true)
                                        .addOption(OptionType.BOOLEAN, TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME,
                                                TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_DESCRIPTION, false),
                                new SubcommandData("revoke", "Revoke a title from a member.").addOption(OptionType.USER,
                                        USER_OPTION_ARG_NAME, USER_OPTION_ARG_DESCRIPTION, true)
                                        .addOption(OptionType.STRING, TITLE_ID_OPTION_ARG_NAME,
                                                TITLE_ID_OPTION_ARG_DESCRIPTION, true)
                                        .addOption(OptionType.BOOLEAN, TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME,
                                                TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_DESCRIPTION, false)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) throws CommandException {
        if (event.getSubcommandName() == null) {
            return;
        }
        if (event.getGuild() == null) {
            sendError(event, "You must be in a guild to use this command.", defaultEmbedProvider);
            return;
        }
        Optional<Character> character = userService
                .getSelectedCharacter(userService.getOrCreateUser(event.getUser()));
        if (character.isEmpty()) {
            sendError(event, "You don't have a character.", defaultEmbedProvider);
            return;
        }
        if (event.getSubcommandGroup() != null && event.getSubcommandGroup().equals("admin")) {
            switch (event.getSubcommandName()) {
                case "refresh" -> executeAdminRefresh(event);
                case "add" -> executeAdminAdd(event);
                case "remove" -> executeAdminRemove(event);
                case "list" -> executeAdminList(event);
                case "grant" -> executeAdminGrant(event);
                case "revoke" -> executeAdminRevoke(event);
            }
        } else {
            switch (event.getSubcommandName()) {
                case "refresh" -> executeRefresh(event);
                case "equip" -> executeEquip(event);
                case "unequip" -> executeUnequip(event);
                case "list" -> executeList(event);
            }
        }
    }

    private void executeRefresh(SlashCommandInteractionEvent event) throws CommandException {
        Member member = event.getMember();
        if (member == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (refreshUserNickname(member)) {
            sendPretty(event, "Nickname refreshed for " + member.getAsMention() + ".", defaultEmbedProvider);
        } else {
            sendError(event, "Could not refresh nickname, possibly due to a permission issue.\nIs your role above the bots highest?", defaultEmbedProvider);
        }
    }

    private void executeEquip(SlashCommandInteractionEvent event) throws CommandException {
        Member member = event.getMember();
        if (member == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        if (!titleService.userHasTitle(title.get(), user)) {
            sendError(event, "You don't have that title.", defaultEmbedProvider);
            return;
        }
        Optional<Character> character = userService.getSelectedCharacter(user);
        character.get().setTitle(title.get());
        characterService.updateCharacter(character.get());
        refreshUserNickname(member);
        sendPretty(event, "Title equipped, you now have title ID `" + title.get().getId() + "` as your title.", defaultEmbedProvider);
    }

    private void executeUnequip(SlashCommandInteractionEvent event) throws CommandException {
        Member member = event.getMember();
        if (member == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        Optional<Character> character = userService.getSelectedCharacter(user);
        character.get().setTitle(null);
        characterService.updateCharacter(character.get());
        refreshUserNickname(member);
        sendPretty(event, "Title unequipped, you now have no title.", defaultEmbedProvider);
    }

    private void executeList(SlashCommandInteractionEvent event) throws CommandException {
        Member target;
        if (event.getOptionsByName(USER_OPTION_ARG_NAME).isEmpty()) {
            target = event.getMember();
        } else {
            target = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsMember();
        }
        if (target == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(target.getUser());
        Optional<Character> character = userService.getSelectedCharacter(user);
        // I originally abbreviated this to the first letter of the first name and last names, but it was ugly.
        // Hope that explains the strange variable name.
        String initials = character.get().getFirstName() + " " + character.get().getLastName();
        List<Title> userTitles = titleService.getUserTitles(user);
        List<Title> characterTitles = titleService.getCharacterTitles(character.get());
        List<Title> titles = new ArrayList<>(userTitles);
        titles.addAll(characterTitles);
        if (titles.isEmpty()) {
            if (event.getUser().equals(target.getUser())) {
                sendPretty(event, "# Titles\nYou do not have any titles.", defaultEmbedProvider);
            } else {
                sendPretty(event, "# Titles\nThis user does not have any titles.", defaultEmbedProvider);
            }
            return;
        }
        String titleIds = titles.stream().map(t -> {
            return "`" + t.getId() + "`: " + (t.getPrefix() != null ? t.getPrefix() : "") + initials
                    + (t.getSuffix() != null ? t.getSuffix() : "");
        }).collect(Collectors.joining("\n- "));
        EmbedBuilder embedBuilder = defaultEmbedProvider.getPrettyEmbedBuilder(event.getJDA().getSelfUser());
        embedBuilder.setDescription("# Titles\n" + titleIds);
        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void executeAdminRefresh(SlashCommandInteractionEvent event) throws CommandException {
        if (replyWithErrorIfMissingPermission(event, event.getMember(), Permission.MODERATE_MEMBERS)) {
            return;
        }
        Member member = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsMember();
        if (member == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (character.isEmpty()) {
            sendError(event, "User does not have a character.", defaultEmbedProvider);
            return;
        }
        if (refreshUserNickname(member)) {
            sendPretty(event, "Nickname refreshed for " + member.getAsMention() + ".", defaultEmbedProvider);
            return;
        } else {
            sendError(event, "Failed to refresh nickname for " + member.getAsMention() + ", possibly due to a permission issue.\nIs the users role above the bots highest?", defaultEmbedProvider);
        }
    }

    private boolean refreshUserNickname(Member member) {
        User user = userService.getOrCreateUser(member.getUser());
        Optional<Character> character = userService.getSelectedCharacter(user);
        if (character.isEmpty()) {
            return false;
        }
        String nickname = character.get().discordFullName();
        try {
            member.getGuild().modifyNickname(member, nickname).queue();
            return true;
        } catch (HierarchyException e) {
            log.warn("Failed to change nickname of " + member.getId() + " to " + nickname + " (TitleCommand)");
            return false;
        }
    }

    private void executeAdminAdd(SlashCommandInteractionEvent event) throws CommandException {
        if (replyWithErrorIfMissingPermission(event, event.getMember(), Permission.MANAGE_SERVER)) {
            return;
        }
        String prefix;
        String suffix;
        if (event.getOptionsByName(TITLE_PREFIX_OPTION_ARG_NAME).isEmpty()) {
            prefix = null;
        } else {
            prefix = event.getOptionsByName(TITLE_PREFIX_OPTION_ARG_NAME).get(0).getAsString();
        }
        if (event.getOptionsByName(TITLE_SUFFIX_OPTION_ARG_NAME).isEmpty()) {
            suffix = null;
        } else {
            suffix = event.getOptionsByName(TITLE_SUFFIX_OPTION_ARG_NAME).get(0).getAsString();
        }
        if (prefix == null && suffix == null) {
            sendError(event, "Title must have a prefix or suffix or both.", defaultEmbedProvider);
            return;
        }
        Title title = titleService.createTitle(prefix, suffix);
        sendPretty(event, "Title created, ID: `" + title.getId() + "`", defaultEmbedProvider);
    }

    private void executeAdminRemove(SlashCommandInteractionEvent event) throws CommandException {
        if (replyWithErrorIfMissingPermission(event, event.getMember(), Permission.MANAGE_SERVER)) {
            return;
        }
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.", defaultEmbedProvider);
            return;
        }
        titleService.deleteTitle(title.get());
        sendPretty(event, "Title deleted.", defaultEmbedProvider);
    }

    private void executeAdminList(SlashCommandInteractionEvent event) throws CommandException {
        if (replyWithErrorIfMissingPermission(event, event.getMember(), Permission.MODERATE_MEMBERS)) {
            return;
        }
        List<Title> titles = titleService.getAllTitles();
        if (titles.isEmpty()) {
            sendPretty(event, "# Titles\nNo titles have been created (yet).", defaultEmbedProvider);
            return;
        }
        // TODO: Add pagination
        String titleIds = titles.stream().map(t -> {
            return "`" + t.getId() + "`: " + (t.getPrefix() != null ? t.getPrefix() : "") + "Firstname Lastname"
                    + (t.getSuffix() != null ? t.getSuffix() : "");
        }).collect(Collectors.joining("\n- "));
        EmbedBuilder embedBuilder = defaultEmbedProvider.getPrettyEmbedBuilder(event.getJDA().getSelfUser());
        embedBuilder.setDescription("# Titles\n" + titleIds);
        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void executeAdminGrant(SlashCommandInteractionEvent event) throws CommandException {
        if (replyWithErrorIfMissingPermission(event, event.getMember(), Permission.MODERATE_MEMBERS)) {
            return;
        }
        Member member = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsMember();
        if (member == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        boolean isCharacter = !event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).isEmpty()
                && event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).get(0).getAsBoolean();
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        if (isCharacter) {
            Character character = userService.getSelectedCharacter(user).get();
            titleService.grantTitle(character, title.get());
        } else {
            titleService.grantTitle(user, title.get());
        }
        sendPretty(event, "Title granted to " + member.getAsMention() + ".", defaultEmbedProvider);
    }

    private void executeAdminRevoke(SlashCommandInteractionEvent event) throws CommandException {
        if (replyWithErrorIfMissingPermission(event, event.getMember(), Permission.MODERATE_MEMBERS)) {
            return;
        }
        Member member = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsMember();
        if (member == null) {
            sendError(event, "Member not found.", defaultEmbedProvider);
            return;
        }
        boolean isCharacter = !event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).isEmpty()
                && event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).get(0).getAsBoolean();
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(member.getUser());
        if (isCharacter) {
            Character character = userService.getSelectedCharacter(user).get();
            titleService.revokeTitle(character, title.get());
        } else {
            titleService.revokeTitle(user, title.get());
        }
        sendPretty(event, "Title revoked from " + member.getAsMention() + ".", defaultEmbedProvider);
    }

    private boolean replyWithErrorIfMissingPermission(SlashCommandInteractionEvent event, Member member,
            Permission permission) {
        if (!member.hasPermission(permission)) {
            sendError(event, "You don't have permission to use this command.\nYou must have the "
                    + "`" + permission.getName() + "` permissions.", defaultEmbedProvider);
            return true;
        }
        return false;
    }
}
