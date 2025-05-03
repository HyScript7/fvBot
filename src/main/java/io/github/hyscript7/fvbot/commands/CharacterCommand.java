package io.github.hyscript7.fvbot.commands;

import java.util.Optional;

import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.ICommand;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import io.github.hyscript7.fvbot.services.UserService;
import io.github.hyscript7.fvbot.data.models.Character;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

@Component
@Slf4j
public class CharacterCommand implements ICommand {

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

    CharacterCommand(IEmbedProvider defaultEmbedProvider, UserService userService) {
        this.defaultEmbedProvider = defaultEmbedProvider;
        this.userService = userService;
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
                        CHARACTER_INNATE_NAME_OPTION_ARG_NAME, CHARACTER_FIRSTNAME_OPTION_ARG_DESCRIPTION, true),
                new SubcommandData("switch", "Switches to another character.").addOption(OptionType.STRING,
                        CHARACTER_INNATE_NAME_OPTION_ARG_NAME, CHARACTER_FIRSTNAME_OPTION_ARG_DESCRIPTION, true))
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
                                        new SubcommandData("create", "Creates a new character for either your self or another user.")
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
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
    }

    private void executeDelete(SlashCommandInteractionEvent event) throws CommandException {
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
    }

    private void executeList(SlashCommandInteractionEvent event) throws CommandException {
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
    }

    private void executeSwitch(SlashCommandInteractionEvent event) throws CommandException {
        if (true) { // ! Remove this block
            sendError(event, "This command is not implemented yet.", defaultEmbedProvider);
            return;
        }
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
