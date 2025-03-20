package io.github.hyscript7.fvbot.config;

import java.util.List;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

@Configuration
@Slf4j
public class JDAConfig {
    @Value("${fvbot.token}")
    private String token;

    private final List<ListenerAdapter> listeners;
    private JDA jda;

    public JDAConfig(List<ListenerAdapter> listeners) {
        this.listeners = listeners;
    }

    @Bean
    public JDABuilder jdaBuilder() {
        JDABuilder jdaBuilder = JDABuilder
                .createDefault(token)
                .setActivity(Activity.watching("the Chambers Realm"))
                .enableIntents(EnumSet.allOf(GatewayIntent.class));
        listeners.forEach(jdaBuilder::addEventListeners);
        return jdaBuilder;
    }

    @Bean
    public JDA jda() {
        this.jda = jdaBuilder().build();
        return this.jda;
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (jda != null) {
            log.info("Shutting down JDA...");
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    log.warn("JDA did not shut down gracefully within 10 seconds!");
                }
            } catch (InterruptedException e) {
                log.error("Thread interrupt received while shutting down JDA!", e);
                throw new InterruptedException(e.getMessage());
            }
        }
    }
}
