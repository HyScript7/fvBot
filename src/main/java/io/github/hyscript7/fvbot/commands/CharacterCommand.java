package io.github.hyscript7.fvbot.commands;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.ICommand;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import io.github.hyscript7.fvbot.services.CharacterService;
import io.github.hyscript7.fvbot.services.InnateNameService;
import io.github.hyscript7.fvbot.services.UserService;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.User;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

@Component
@Slf4j
public class CharacterCommand implements ICommand {

    private final CharacterService characterService;

    private final InnateNameService innateNameService;

    private final IEmbedProvider defaultEmbedProvider;
    private final UserService userService;
    private static final String NAME = "character";
    private static final String DESCRIPTION = "Provides sub-commands for you to work with characters.";

    private static final String USER_OPTION_ARG_NAME = "user";
    private static final String USER_OPTION_ARG_DESCRIPTION = "User to list or manage titles of.";
    private static final String TITLE_ID_OPTION_ARG_NAME = "id";
    private static final String TITLE_ID_OPTION_ARG_DESCRIPTION = "Id of the title.";
    private static final String CHARACTER_ID_OPTION_ARG_NAME = "id";
    private static final String CHARACTER_ID_OPTION_ARG_DESCRIPTION = "Id of the character.";
    private static final String CHARACTER_INNATE_NAME_OPTION_ARG_NAME = "innate";
    private static final String CHARACTER_INNATE_NAME_OPTION_ARG_DESCRIPTION = "Innate name of the character.";
    private static final String CHARACTER_FIRSTNAME_OPTION_ARG_NAME = "firstname";
    private static final String CHARACTER_FIRSTNAME_OPTION_ARG_DESCRIPTION = "First name of the character.";
    private static final String CHARACTER_LASTNAME_OPTION_ARG_NAME = "lastname";
    private static final String CHARACTER_LASTNAME_OPTION_ARG_DESCRIPTION = "Last name of the character.";
    private static final String CHARACTER_RESURRECTION_OPTION_ARG_NAME = "resurrection";
    private static final String CHARACTER_RESURRECTION_OPTION_ARG_DESCRIPTION = "Character resurrection status.";
    private static final String CHARACTER_LEVEL_OPTION_ARG_NAME = "level";
    private static final String CHARACTER_LEVEL_OPTION_ARG_DESCRIPTION = "Character level.";

    private static final int RANDOM_INNATE_NAME_MIN_LENGTH = 4;
    private static final int RANDOM_INNATE_NAME_MAX_LENGTH = 9;

    CharacterCommand(IEmbedProvider defaultEmbedProvider, UserService userService, InnateNameService innateNameService,
            CharacterService characterService) {
        this.defaultEmbedProvider = defaultEmbedProvider;
        this.userService = userService;
        this.innateNameService = innateNameService;
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
                new SubcommandData("create", "Creates a new character.")
                        .addOption(OptionType.STRING, CHARACTER_FIRSTNAME_OPTION_ARG_NAME,
                                CHARACTER_FIRSTNAME_OPTION_ARG_DESCRIPTION, true)
                        .addOption(OptionType.STRING, CHARACTER_LASTNAME_OPTION_ARG_NAME,
                                CHARACTER_LASTNAME_OPTION_ARG_DESCRIPTION, true)
                        .addOption(OptionType.STRING, CHARACTER_RESURRECTION_OPTION_ARG_NAME,
                                CHARACTER_RESURRECTION_OPTION_ARG_DESCRIPTION, false)
                        .addOption(OptionType.INTEGER, CHARACTER_LEVEL_OPTION_ARG_NAME,
                                CHARACTER_LEVEL_OPTION_ARG_DESCRIPTION, false)
                        .addOption(OptionType.STRING, TITLE_ID_OPTION_ARG_NAME, TITLE_ID_OPTION_ARG_DESCRIPTION, false),
                new SubcommandData("list", "Lists all of your (or the specified user's) characters.")
                        .addOption(OptionType.USER, USER_OPTION_ARG_NAME, USER_OPTION_ARG_DESCRIPTION, false),
                new SubcommandData("delete", "Deletes a character.").addOption(OptionType.STRING,
                        CHARACTER_INNATE_NAME_OPTION_ARG_NAME, CHARACTER_INNATE_NAME_OPTION_ARG_DESCRIPTION, true),
                new SubcommandData("switch", "Switches to another character.").addOption(OptionType.STRING,
                        CHARACTER_INNATE_NAME_OPTION_ARG_NAME, CHARACTER_INNATE_NAME_OPTION_ARG_DESCRIPTION, true))
                .addSubcommandGroups(
                        new SubcommandGroupData("admin", "Admin commands for managing characters.")
                                .addSubcommands(
                                        new SubcommandData("list",
                                                "Lists all of your (or the specified user's) characters. (By IDs)")
                                                .addOption(OptionType.USER, USER_OPTION_ARG_NAME,
                                                        USER_OPTION_ARG_DESCRIPTION, false),
                                        new SubcommandData("set", "Sets your or another user's character. (By ID)")
                                                .addOption(OptionType.STRING, CHARACTER_ID_OPTION_ARG_NAME,
                                                        CHARACTER_ID_OPTION_ARG_DESCRIPTION, true)
                                                .addOption(OptionType.USER, USER_OPTION_ARG_NAME,
                                                        USER_OPTION_ARG_DESCRIPTION, false),
                                        // This create is basically the same as the normal one, but you can specify
                                        // another member and the innate name.
                                        new SubcommandData("create",
                                                "Creates a new character for either your self or another user.")
                                                .addOption(OptionType.USER, USER_OPTION_ARG_NAME,
                                                        USER_OPTION_ARG_DESCRIPTION, true)
                                                .addOption(OptionType.STRING, CHARACTER_FIRSTNAME_OPTION_ARG_NAME,
                                                        CHARACTER_FIRSTNAME_OPTION_ARG_DESCRIPTION, true)
                                                .addOption(OptionType.STRING, CHARACTER_LASTNAME_OPTION_ARG_NAME,
                                                        CHARACTER_LASTNAME_OPTION_ARG_DESCRIPTION, true)
                                                .addOption(OptionType.STRING, CHARACTER_INNATE_NAME_OPTION_ARG_NAME,
                                                        CHARACTER_INNATE_NAME_OPTION_ARG_DESCRIPTION, false)
                                                .addOption(OptionType.STRING, CHARACTER_RESURRECTION_OPTION_ARG_NAME,
                                                        CHARACTER_RESURRECTION_OPTION_ARG_DESCRIPTION, false)
                                                .addOption(OptionType.INTEGER, CHARACTER_LEVEL_OPTION_ARG_NAME,
                                                        CHARACTER_LEVEL_OPTION_ARG_DESCRIPTION, false)
                                                .addOption(OptionType.STRING, TITLE_ID_OPTION_ARG_NAME,
                                                        TITLE_ID_OPTION_ARG_DESCRIPTION, false),
                                        new SubcommandData("delete", "Deletes a character (by ID).").addOption(
                                                OptionType.STRING, CHARACTER_ID_OPTION_ARG_NAME,
                                                CHARACTER_ID_OPTION_ARG_DESCRIPTION, true)));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) throws CommandException {
        if (event.getSubcommandName() == null) {
            sendError(event, "You must specify a subcommand.", defaultEmbedProvider);
            return;
        }
        if (event.getGuild() == null || event.getMember() == null) {
            sendError(event, "You must be in a guild to use this command.", defaultEmbedProvider);
            return;
        }
        if (event.getSubcommandGroup() != null && event.getSubcommandGroup().equals("admin")) {
            if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
                sendError(event,
                        "You don't have permission to use this command.\nYou must have the `MODERATE_MEMBERS` permission.",
                        defaultEmbedProvider);
                return;
            }
            switch (event.getSubcommandName()) {
                case "create" -> executeAdminCreate(event);
                case "delete" -> executeAdminDelete(event);
                case "list" -> executeAdminList(event);
                case "set" -> executeAdminSet(event);
            }
        } else {
            // Makes sure the user has a character
            Optional<Character> character = userService
                    .getSelectedCharacter(userService.getOrCreateUser(event.getUser()));
            if (character.isEmpty()) {
                sendError(event, "You don't have a character.", defaultEmbedProvider);
                return;
            }
            switch (event.getSubcommandName()) {
                case "create" -> executeCreate(event);
                case "delete" -> executeDelete(event);
                case "list" -> executeList(event);
                case "switch" -> executeSwitch(event);
            }
        }
    }

    private void executeCreate(SlashCommandInteractionEvent event) throws CommandException {
        String firstName = event.getOption(CHARACTER_FIRSTNAME_OPTION_ARG_NAME).getAsString();
        String lastName = event.getOption(CHARACTER_LASTNAME_OPTION_ARG_NAME).getAsString();
        String resurrection;
        if (!event.getOptionsByName(CHARACTER_RESURRECTION_OPTION_ARG_NAME).isEmpty()) {
            resurrection = event.getOption(CHARACTER_RESURRECTION_OPTION_ARG_NAME).getAsString();
        } else {
            resurrection = "0";
        }
        String level;
        if (!event.getOptionsByName(CHARACTER_LEVEL_OPTION_ARG_NAME).isEmpty()) {
            level = event.getOption(CHARACTER_LEVEL_OPTION_ARG_NAME).getAsString();
        } else {
            level = "1";
        }
        int resurrectionInt;
        int levelInt;
        try {
            resurrectionInt = Integer.parseInt(resurrection);
            levelInt = Integer.parseInt(level);
        } catch (NumberFormatException e) {
            sendError(event, "Resurrection and level must be integers.", defaultEmbedProvider);
            return;
        }
        User user = userService.getOrCreateUser(event.getUser());

        String name = innateNameService.generateInnateName(RANDOM_INNATE_NAME_MIN_LENGTH,
                RANDOM_INNATE_NAME_MAX_LENGTH);

        Character character = Character.builder()
                .innateName(name)
                .firstName(firstName)
                .lastName(lastName)
                .resurrection(resurrectionInt)
                .level(levelInt)
                .user(user)
                .build();
        characterService.createCharacter(user, character);
        event.getHook().editOriginalEmbeds(defaultEmbedProvider.getPrettyEmbedBuilder(event.getJDA().getSelfUser())
                .setDescription("Character created.").build()).queue();
    }

    private void executeDelete(SlashCommandInteractionEvent event) throws CommandException {
        String innateName;
        if (event.getOptionsByName(CHARACTER_INNATE_NAME_OPTION_ARG_NAME).isEmpty()) {
            sendError(event, "You must specify an innate name.", defaultEmbedProvider);
            return;
        }
        innateName = event.getOption(CHARACTER_INNATE_NAME_OPTION_ARG_NAME).getAsString();
        User user = userService.getOrCreateUser(event.getUser());
        Optional<Character> character = characterService.getCharactersOfUser(user).stream()
                .filter(c -> c.getInnateName().equals(innateName)).findFirst();
        if (character.isEmpty()) {
            sendError(event, "Character not found.", defaultEmbedProvider);
            return;
        }
        if (user.getCurrentCharacter().getInnateName().equals(character.get().getInnateName())) {
            sendError(event, "You can't delete a currently selected character.", defaultEmbedProvider);
            return;
        }
        characterService.deleteCharacter(character.get());
        event.getHook().editOriginalEmbeds(defaultEmbedProvider.getPrettyEmbedBuilder(event.getJDA().getSelfUser())
                .setDescription("Character deleted.").build()).queue();
    }

    private void executeList(SlashCommandInteractionEvent event) throws CommandException {
        net.dv8tion.jda.api.entities.User discordUser;
        if (event.getOptionsByName(USER_OPTION_ARG_NAME).isEmpty()) {
            discordUser = event.getUser();
        } else {
            discordUser = event.getOptionsByName(USER_OPTION_ARG_NAME).get(0).getAsUser();
        }
        User user = userService.getOrCreateUser(discordUser);
        List<Character> characters = characterService.getCharactersOfUser(user);
        if (characters.isEmpty()) {
            if (discordUser.equals(event.getUser())) {
                sendError(event, "You don't have any characters.", defaultEmbedProvider);
            } else {
                sendError(event, user.getUsername() + " doesn't have any characters.", defaultEmbedProvider);
            }
            return;
        }
        Character currentCharacter = user.getCurrentCharacter();
        if (currentCharacter == null) {
            if (discordUser.equals(event.getUser())) {
                sendError(event, "You don't have a character.", defaultEmbedProvider);
            } else {
                sendError(event, user.getUsername() + " doesn't have a character.", defaultEmbedProvider);
            }
            return;
        }
        EmbedBuilder embedBuilder = defaultEmbedProvider.getPrettyEmbedBuilder(event.getJDA().getSelfUser())
                .setTitle("Characters of " + discordUser.getName());
        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : characters) {
            stringBuilder
                    .append("\n- " + (currentCharacter.getInnateName().equals(character.getInnateName()) ? "**" : "")
                            + character.getFullNameWithTitle() + " ("
                            + (event.getUser().equals(discordUser) ? character.getInnateName() : character.getId())
                            + ")" + (currentCharacter.getInnateName().equals(character.getInnateName()) ? "**" : ""));
        }
        embedBuilder.setDescription(stringBuilder.toString().strip());
        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
    }

    private void executeSwitch(SlashCommandInteractionEvent event) throws CommandException {
        String innateName;
        if (event.getOptionsByName(CHARACTER_INNATE_NAME_OPTION_ARG_NAME).isEmpty()) {
            sendError(event, "You must specify an innate name.", defaultEmbedProvider);
            return;
        }
        innateName = event.getOption(CHARACTER_INNATE_NAME_OPTION_ARG_NAME).getAsString();
        User user = userService.getOrCreateUser(event.getUser());
        Optional<Character> character = characterService.getCharactersOfUser(user).stream()
                .filter(c -> c.getInnateName().equals(innateName)).findFirst();
        if (character.isEmpty()) {
            sendError(event, "Character not found.", defaultEmbedProvider);
            return;
        }
        userService.setCurrentCharacter(user, character.get());
        String nickname = character.get().discordFullName();
        try {
            event.getMember().modifyNickname(nickname).queue();
        } catch (HierarchyException e) {
            log.warn("Failed to change nickname of " + event.getMember().getId() + " to " + nickname
                    + " (CharacterCommand)");
        }
        event.getHook().editOriginalEmbeds(defaultEmbedProvider.getPrettyEmbedBuilder(event.getJDA().getSelfUser())
                .setDescription("Character switched.").build()).queue();
    }

    private void executeAdminList(SlashCommandInteractionEvent event) throws CommandException {
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
    }

    private void executeAdminSet(SlashCommandInteractionEvent event) throws CommandException {
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
    }

    private void executeAdminCreate(SlashCommandInteractionEvent event) throws CommandException {
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
    }

    private void executeAdminDelete(SlashCommandInteractionEvent event) throws CommandException {
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
    }
}
