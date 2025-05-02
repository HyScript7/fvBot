package io.github.hyscript7.fvbot.core.commands;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * The interface for a slash command.
 */
public interface ICommand {
    /**
     * The name of the command.
     *
     * @return the name of the command.
     */
    String getName();

    /**
     * The description of the command.
     *
     * @return the description of the command.
     */
    String getDescription();

    /**
     * The slash command data of the command.
     *
     * @return the slash command data of the command.
     */
    default SlashCommandData getSlashCommandData() {
        return Commands.slash(getName(), getDescription());
    }

    /**
     * Execute the command.
     * ! This presumes the interaction has already been made ephemeral and deferred.
     *
     * @param event the event of the command.
     * @throws CommandException if the command execution error.
     */
    void execute(SlashCommandInteractionEvent event) throws CommandException;

    default boolean prefersEphemeral() {
        return true;
    }

    default void sendError(SlashCommandInteractionEvent event, String message,
            IEmbedProvider embedProvider) {
        event.getHook()
                .editOriginalEmbeds(embedProvider.getErrorEmbedBuilder(event.getJDA()).setDescription(message).build())
                .queue();
    }
}
