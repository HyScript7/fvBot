package io.github.hyscript7.fvbot.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import io.github.hyscript7.fvbot.core.commands.ICommand;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;
import io.github.hyscript7.fvbot.core.exceptions.commands.CommandException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PingCommand implements ICommand {
    private final String name = "ping";
    private final String description = "Responds with the current API lantency in miliseconds.";

    private IEmbedProvider embedProvider;

    public PingCommand(@Autowired IEmbedProvider embedProvider) {
        this.embedProvider = embedProvider;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) throws CommandException {
        long apiLatencyMs = event.getJDA().getRestPing().complete();
        long gatewayLatencyMs = event.getJDA().getGatewayPing();
        EmbedBuilder embedBuilder = embedProvider.getPrettyEmbedBuilder(event.getJDA().getSelfUser());
        embedBuilder.addField("API Latency", apiLatencyMs + " ms", false);
        embedBuilder.addField("Gateway Latency", gatewayLatencyMs + " ms", false);
        event.getHook().editOriginalEmbeds(embedBuilder.build()).queue();
        log.info("Latency (API): " + apiLatencyMs + " ms");
        log.info("Latency (Gateway): " + gatewayLatencyMs + " ms");
    }
}
