package io.github.hyscript7.fvbot.services;

import java.util.List;
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

    public Optional<Character> getCharacterById(Long id) {
        return characterRepository.findById(id);
    }

    public List<Character> getCharactersOfUser(User user) {
        return characterRepository.findByUser(user);
    }

    public void updateCharacter(Character character) {
        characterRepository.save(character);
    }

    public void updateInnateName(Character character, String innateName) {
        character.setInnateName(innateName);
        characterRepository.save(character);
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

    public void deleteCharacter(Character character) {
        characterRepository.delete(character);
    }

}
