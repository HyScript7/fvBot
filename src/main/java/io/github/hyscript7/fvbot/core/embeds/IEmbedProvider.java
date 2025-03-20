package io.github.hyscript7.fvbot.core.embeds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

public interface IEmbedProvider {
    /**
     * Creates a default embed builder with a colorful design and footer timestamp.
     *
     * @return the default embed builder.
     */
    EmbedBuilder getEmbedBuilder();

    /**
     * Creates a pretty embed builder with a colorful design, footer timestamp,
     * and a thumbnail image of the user's avatar. Sets the author to the user.
     *
     * @param user the user whose avatar is to be used as the thumbnail image.
     * @return the pretty embed builder customized with the user's avatar.
     */
    EmbedBuilder getPrettyEmbedBuilder(User user);

    /**
     * Creates an error embed builder with a red design, footer timestamp,
     * and no image. Sets the author to the bot.
     *
     * @return the error embed builder.
     */
    EmbedBuilder getErrorEmbedBuilder(JDA jda);
}
