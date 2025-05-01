package io.github.hyscript7.fvbot.services;

import java.util.Optional;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.repositories.CharacterRepository;
import org.springframework.stereotype.Service;

import io.github.hyscript7.fvbot.data.models.Title;
import io.github.hyscript7.fvbot.data.models.User;

@Service
public class CharacterService {

    private final CharacterRepository characterRepository;

    public CharacterService(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    public void setCharacterTitle(Character character, Title title) {
        character.setTitle(title);
        characterRepository.save(character);
    }

    public Optional<Title> getCharacterTitle(Character character) {
        return Optional.ofNullable(character.getTitle());
    }

    public Character createCharacter(User user, Character character) {
        character.setUser(user);
        return characterRepository.save(character);
    }
    
}
