package io.github.hyscript7.fvbot.events;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.CommandService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

@Component
@Slf4j
public class CommandSyncHandler extends ListenerAdapter {
    CommandService commandService;

    public CommandSyncHandler(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Gets the difference between local and remote commands. This is used to
     * determine whether the commands need to be synced.
     *
     * @param localCommands  the local commands.
     * @param remoteCommands the remote commands.
     * @return the difference between local and remote commands. 0 means there is no
     *         difference.
     */
    private long getDifference(List<Command> localCommands, List<Command> remoteCommands) {
        List<String> allCommandNames = localCommands.stream().map(c -> c.getName()).toList();
        List<String> syncedCommandNames = remoteCommands.stream().map(c -> c.getName()).toList();
        long missingOnRemote = allCommandNames.stream().filter(c -> !syncedCommandNames.contains(c)).count();
        long missingOnLocal = syncedCommandNames.stream().filter(c -> !allCommandNames.contains(c)).count();
        return missingOnRemote + missingOnLocal;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<Command> localCommands = commandService.getCommands().stream()
                .map(c -> (Command) c.getSlashCommandData()).toList();

        event.getJDA().retrieveCommands().onSuccess(syncedCommands -> {
            long difference = getDifference(localCommands, syncedCommands);
            if (difference > 0l) {
                // Sync the commands globally.
                log.info("Syncing commands with a difference of {} commands", difference);
                commandService.syncCommands(event.getJDA());
            }
        }).queue();
    }
}
