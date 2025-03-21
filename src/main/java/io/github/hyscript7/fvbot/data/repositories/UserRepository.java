package io.github.hyscript7.fvbot.data.repositories;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.User;

public interface UserRepository extends Repository<User, Long> {
    Optional<User> findByDiscordId(Long discordId);

    User save(User user);

    void delete(User user);

    boolean existsByDiscordId(Long discordId);

    Optional<User> findByCurrentCharacter(Character character);
}
