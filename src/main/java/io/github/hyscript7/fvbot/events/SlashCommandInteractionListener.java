package io.github.hyscript7.fvbot.events;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.CommandService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
public class SlashCommandInteractionListener extends ListenerAdapter {
    CommandService commandService;

    public SlashCommandInteractionListener(CommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        commandService.executeCommand(event);
    }

}
