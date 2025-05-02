package io.github.hyscript7.fvbot.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.ICommand;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.Title;
import io.github.hyscript7.fvbot.data.models.User;
import io.github.hyscript7.fvbot.services.CharacterService;
import io.github.hyscript7.fvbot.services.TitleService;
import io.github.hyscript7.fvbot.services.UserService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

@Component
@Slf4j
public class TitleCommand implements ICommand {

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

    TitleCommand(TitleService titleService, UserService userService, CharacterService characterService) {
        this.titleService = titleService;
        this.userService = userService;
        this.characterService = characterService;
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
                new SubcommandData("equip", "Equips a title.").addOption(OptionType.STRING, TITLE_ID_OPTION_ARG_NAME,
                        TITLE_ID_OPTION_ARG_DESCRIPTION, true),
                new SubcommandData("unequip", "Unequips your current title."),
                new SubcommandData("list", "Lists all of your titles.").addOption(OptionType.USER, USER_OPTION_ARG_NAME,
                        USER_OPTION_ARG_DESCRIPTION, false))
                .addSubcommandGroups(
                        new SubcommandGroupData("admin", "Admin commands for managing titles.").addSubcommands(
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
        Optional<Character> character = userService
                .getSelectedCharacter(userService.getOrCreateUser(event.getUser().getIdLong()));
        if (character.isEmpty()) {
            sendError(event, "You don't have a character.");
            return;
        }
        if (event.getSubcommandGroup() != null && event.getSubcommandGroup().equals("admin")) {
            // TODO: Check whether role has permission
            switch (event.getSubcommandName()) {
                case "add" -> executeAdminAdd(event);
                case "remove" -> executeAdminRemove(event);
                case "list" -> executeAdminList(event);
                case "grant" -> executeAdminGrant(event);
                case "revoke" -> executeAdminRevoke(event);
            }
        } else {
            switch (event.getSubcommandName()) {
                case "equip" -> executeEquip(event);
                case "unequip" -> executeUnequip(event);
                case "list" -> executeList(event);
            }
        }
    }

    private void executeEquip(SlashCommandInteractionEvent event) throws CommandException {
        Member member = event.getMember();
        if (member == null) {
            sendError(event, "Member not found.");
            return;
        }
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.");
            return;
        }
        User user = userService.getOrCreateUser(member.getIdLong());
        if (!titleService.userHasTitle(title.get(), user)) {
            sendError(event, "You don't have that title.");
            return;
        }
        Optional<Character> character = userService.getSelectedCharacter(user);
        character.get().setTitle(title.get());
        characterService.updateCharacter(character.get());
        event.getHook().editOriginal("Title equipped.").queue();
    }

    private void executeUnequip(SlashCommandInteractionEvent event) throws CommandException {
        Member member = event.getMember();
        if (member == null) {
            sendError(event, "Member not found.");
            return;
        }
        User user = userService.getOrCreateUser(member.getIdLong());
        Optional<Character> character = userService.getSelectedCharacter(user);
        character.get().setTitle(null);
        characterService.updateCharacter(character.get());
        event.getHook().editOriginal("Title unequipped.").queue();
    }

    private void executeList(SlashCommandInteractionEvent event) throws CommandException {
        Member target;
        if (event.getOptionsByName(USER_OPTION_ARG_NAME).isEmpty()) {
            target = event.getMember();
        } else {
            target = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsMember();
        }
        if (target == null) {
            sendError(event, "Member not found.");
            return;
        }
        User user = userService.getOrCreateUser(target.getIdLong());
        Optional<Character> character = userService.getSelectedCharacter(user);
        String initials = character.get().getFirstName().charAt(0) + "" + character.get().getLastName().charAt(0);
        List<Title> userTitles = titleService.getUserTitles(user);
        List<Title> characterTitles = titleService.getCharacterTitles(character.get());
        List<Title> titles = new ArrayList<>(userTitles);
        titles.addAll(characterTitles);
        if (titles.isEmpty()) {
            event.getHook().editOriginal("Titles: None").queue();
            return;
        }
        String titleIds = titles.stream().map(t -> {
            return "`" + t.getId() + "`: " + t.getPrefix() + initials + t.getSuffix();
        }).collect(Collectors.joining("\n- "));
        event.getHook().editOriginal("Titles: " + titleIds).queue();
    }

    private void executeAdminAdd(SlashCommandInteractionEvent event) throws CommandException {
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
            sendError(event, "Title must have a prefix or suffix or both.");
            return;
        }
        Title title = titleService.createTitle(prefix, suffix);
        event.getHook().editOriginal("Title created with ID `" + title.getId() + "`.").queue();
    }

    private void executeAdminRemove(SlashCommandInteractionEvent event) throws CommandException {
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.");
            return;
        }
        titleService.deleteTitle(title.get());
        event.getHook().editOriginal("Title deleted.").queue();
    }

    private void executeAdminList(SlashCommandInteractionEvent event) throws CommandException {
        List<Title> titles = titleService.getAllTitles();
        if (titles.isEmpty()) {
            event.getHook().editOriginal("Titles: None").queue();
            return;
        }
        // TODO: Add pagination
        String titleIds = titles.stream().map(t -> {
            return "`" + t.getId() + "`: " + t.getPrefix() + "Firstname Lastname" + t.getSuffix();
        }).collect(Collectors.joining("\n- "));
        event.getHook().editOriginal("Titles: " + titleIds).queue();
    }

    private void executeAdminGrant(SlashCommandInteractionEvent event) throws CommandException {
        Member member = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsMember();
        if (member == null) {
            sendError(event, "Member not found.");
            return;
        }
        boolean isCharacter = !event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).isEmpty()
                && event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).get(0).getAsBoolean();
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.");
            return;
        }
        User user = userService.getOrCreateUser(member.getIdLong());
        if (isCharacter) {
            Character character = userService.getSelectedCharacter(user).get();
            titleService.grantTitle(character, title.get());
        } else {
            titleService.grantTitle(user, title.get());
        }
        event.getHook().editOriginal("Title granted.").queue();
    }

    private void executeAdminRevoke(SlashCommandInteractionEvent event) throws CommandException {
        Member member = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsMember();
        if (member == null) {
            sendError(event, "Member not found.");
            return;
        }
        boolean isCharacter = !event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).isEmpty()
                && event.getOptionsByName(TITLE_GRANT_CHARACTER_ONLY_OPTION_ARG_NAME).get(0).getAsBoolean();
        String titleId = event.getOptionsByName(TITLE_ID_OPTION_ARG_NAME).get(0).getAsString();
        Optional<Title> title = titleService.getTitleById(Long.parseLong(titleId));
        if (title.isEmpty()) {
            sendError(event, "Title not found.");
            return;
        }
        User user = userService.getOrCreateUser(member.getIdLong());
        if (isCharacter) {
            Character character = userService.getSelectedCharacter(user).get();
            titleService.revokeTitle(character, title.get());
        } else {
            titleService.revokeTitle(user, title.get());
        }
        event.getHook().editOriginal("Title revoked.").queue();
    }

    private void sendError(SlashCommandInteractionEvent event, String message) {
        event.getHook().editOriginal(message).queue();
    }
}
