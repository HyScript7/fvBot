package io.github.hyscript7.fvbot.services;

import java.util.Optional;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.repositories.CharacterRepository;
import io.github.hyscript7.fvbot.data.repositories.UserRepository;
import org.springframework.stereotype.Service;

import io.github.hyscript7.fvbot.data.models.User;

@Service
public class UserService {

    private final CharacterRepository characterRepository;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, CharacterRepository characterRepository) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
    }

    public User getOrCreateUser(Long discordId) {
        Optional<User> user = userRepository.findByDiscordId(discordId);
        return user.orElseGet(() -> {
            User newUser = User.builder().discordId(discordId).username("").build();
            return userRepository.save(newUser);
        });
    }

    public User getOrCreateUser(net.dv8tion.jda.api.entities.User discordUser) {
        Optional<User> user = userRepository.findByDiscordId(discordUser.getIdLong());
        return user.orElseGet(() -> {
            User newUser = User.builder().discordId(discordUser.getIdLong()).username(discordUser.getName()).build();
            return userRepository.save(newUser);
        });
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    public boolean hasCharacters(User user) {
        return !characterRepository.findByUser(user).isEmpty();
    }

    public Optional<Character> getSelectedCharacter(User user) {
        return Optional.ofNullable(user.getCurrentCharacter());
    }

    public void setCurrentCharacter(User user, Character character) {
        user.setCurrentCharacter(character);
        userRepository.save(user);
    }

}
