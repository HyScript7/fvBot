package io.github.hyscript7.fvbot.core.embeds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class EmbedProviderImpl implements IEmbedProvider {

    private static final Color FUSIONVERSE_PURPLE = Color.decode("#8C18FF");
    private static final Color ERROR_RED = Color.RED;

    @Override
    public EmbedBuilder getEmbedBuilder() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(FUSIONVERSE_PURPLE);
        embedBuilder.setTimestamp(getUtcNow());
        return embedBuilder;
    }

    private EmbedBuilder getAuthorEmbedBuilder(User user) {
        EmbedBuilder embedBuilder = getEmbedBuilder();
        embedBuilder.setAuthor(user.getName(), null, user.getAvatarUrl());
        return embedBuilder;
    }

    @Override
    public EmbedBuilder getPrettyEmbedBuilder(User user) {
        EmbedBuilder embedBuilder = getAuthorEmbedBuilder(user);
        embedBuilder.setThumbnail(user.getAvatarUrl());
        return embedBuilder;
    }

    @Override
    public EmbedBuilder getErrorEmbedBuilder(JDA jda) {
        EmbedBuilder embedBuilder = getAuthorEmbedBuilder(jda.getSelfUser());
        embedBuilder.setColor(ERROR_RED);
        return embedBuilder;
    }

    private LocalDateTime getUtcNow() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }

}
