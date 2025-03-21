package io.github.hyscript7.fvbot.core.commands;

import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

@Service
@Slf4j
public class CommandService {
    List<ICommand> commands;
    IEmbedProvider embedProvider;

    public CommandService(@Autowired List<ICommand> commands, @Autowired IEmbedProvider embedProvider) {
        this.commands = commands;
        this.embedProvider = embedProvider;
    }

    @PostConstruct
    public void postConstruct() {
        log.info("CommandService loaded with " + commands.size() + " commands.");
    }

    /**
     * @return all commands
     */
    public List<ICommand> getCommands() {
        return commands;
    }

    /**
     * Gets the command with the given name.
     *
     * @param name the name of the command.
     * @return the command with the given name, or null if not found.
     */
    public @Nullable ICommand getCommand(String name) {
        return commands.stream().filter(command -> command.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * Execute the command for the given interaction event.
     * Responds with an error message if the command execution fails.
     * 
     * @param event the interaction event.
     */
    public void executeCommand(SlashCommandInteractionEvent event) {
        // Subcommands will be handled inside of the root command's object.
        // While I could define a new interface for subcommands, I don't think it's
        // necessary at this point.
        // If I do decide to add subcommands, this will need to be changed.
        ICommand command = getCommand(event.getName());
        if (command != null) {
            log.info("Executing command " + command.getName());
            // Only defer the reply if the command prefers ephemeral. (True by default) @see
            // ICommand
            event.deferReply(command.prefersEphemeral()).queue();
            try {
                command.execute(event);
                log.info("Command " + command.getName() + " has finished execution successfully.");
            } catch (CommandException e) {
                log.error("Error executing command " + command.getName(), e);
                handleCommandError(event, e);
            }
        } else {
            log.info("No command found with name " + event.getName());
            // Always defer the reply if no command is found. No one wants to stare at error
            // messages in general chat.
            event.deferReply(true).queue();
            handleUnknownCommand(event);
        }
    }

    private CommandListUpdateAction populateCommandListUpdateAction(CommandListUpdateAction commandListUpdateAction) {
        List<SlashCommandData> slashCommandDataList = getCommands().stream().map(ICommand::getSlashCommandData)
                .collect(Collectors.toList());
        commandListUpdateAction.addCommands(slashCommandDataList);
        return commandListUpdateAction;
    }

    /**
     * @return a {@link CommandListUpdateAction} that updates the global command
     *         list when queued.
     */
    public CommandListUpdateAction getGlobalCommandListUpdateAction(JDA jda) {
        CommandListUpdateAction commandListUpdateAction = jda.updateCommands();
        return populateCommandListUpdateAction(commandListUpdateAction);
    }

    /**
     * @param guild the guild to update the command list for.
     * @return a {@link CommandListUpdateAction} that updates the command list for
     *         the given guild when queued.
     */
    public CommandListUpdateAction getGuildCommandListUpdateAction(Guild guild) {
        CommandListUpdateAction commandListUpdateAction = guild.updateCommands();
        return populateCommandListUpdateAction(commandListUpdateAction);
    }

    /**
     * Syncs the global command list with the commands in this service.
     */
    public void syncCommands(JDA jda) {
        getGlobalCommandListUpdateAction(jda).queue();
    }

    /**
     * Syncs the command list for the given guild with the commands in this service.
     *
     * @param guild the guild to sync the command list for
     */
    public void syncCommands(Guild guild) {
        getGuildCommandListUpdateAction(guild).queue();
    }

    private void handleUnknownCommand(SlashCommandInteractionEvent event) {
        event.reply(MessageCreateData.fromEmbeds(
                embedProvider.getErrorEmbedBuilder(event.getJDA()).setTitle("Error: Unknown Command")
                        .setDescription("Unknown command `" + event.getName().strip() + "`. " + "\n"
                                + "Please use `/help` to see all commands.")
                        .build()))
                .queue();
    }

    private void handleCommandError(SlashCommandInteractionEvent event, CommandException error) {
        event.reply(MessageCreateData.fromEmbeds(
                embedProvider.getErrorEmbedBuilder(event.getJDA()).setTitle("Error: An unexpected exception occurred")
                        .setDescription("The command `" + event.getName().strip() + "` has thrown an exception." + "\n"
                                + "```" + "\n" + error.getMessage() + "\n" + "```" + "\n\n"
                                + "If this error persists, please report it to the developer.\nFor command usage, use `/help "
                                + event.getName().strip() + "`.")
                        .build()))
                .queue();
    }
}
