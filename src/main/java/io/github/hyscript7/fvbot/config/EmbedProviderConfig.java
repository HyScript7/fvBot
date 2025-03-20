package io.github.hyscript7.fvbot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.hyscript7.fvbot.core.embeds.EmbedProviderImpl;
import io.github.hyscript7.fvbot.core.embeds.IEmbedProvider;

@Configuration
public class EmbedProviderConfig {
    @ConditionalOnMissingBean(IEmbedProvider.class)
    @Bean
    public IEmbedProvider defaultEmbedProvider() {
        return new EmbedProviderImpl();
    }
}
