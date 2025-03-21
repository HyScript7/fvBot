package io.github.hyscript7.fvbot.events;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import io.github.hyscript7.fvbot.core.commands.CommandService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.data.DataObject;

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
    private long getDifference(List<SlashCommandData> localCommands, List<SlashCommandData> remoteCommands) {
        List<DataObject> localCommandData = localCommands.stream().map(SlashCommandData::toData).toList();
        List<DataObject> remoteCommandData = remoteCommands.stream().map(SlashCommandData::toData).toList();
        log.debug("Local commands: \n"
                + String.join("\n ", localCommandData.stream().map(DataObject::toPrettyString).toList()));
        log.debug("Remote commands: \n"
                + String.join("\n ", remoteCommandData.stream().map(DataObject::toPrettyString).toList()));
        long missingOnRemote = localCommandData.stream().filter(c -> !remoteCommandData.contains(c)).count();
        long missingOnLocal = remoteCommandData.stream().filter(c -> !localCommandData.contains(c)).count();
        return missingOnRemote + missingOnLocal;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        List<SlashCommandData> localCommands = commandService.getCommands().stream()
                .map(c -> (SlashCommandData) c.getSlashCommandData()).toList();

        event.getJDA().retrieveCommands().onSuccess(syncedCommands -> {
            List<SlashCommandData> remoteCommands = syncedCommands.stream().map(SlashCommandData::fromCommand).toList();

            long difference = getDifference(localCommands, remoteCommands);
            if (difference > 0l) {
                // Sync the commands globally.
                log.info("Syncing commands with a difference of {} commands", difference);
                commandService.syncCommands(event.getJDA());
            } else {
                log.info("Commands are up-to-date!");
            }
        }).queue();
    }
}
