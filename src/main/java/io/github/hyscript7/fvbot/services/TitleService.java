package io.github.hyscript7.fvbot.services;

import io.github.hyscript7.fvbot.data.repositories.CharacterRepository;
import io.github.hyscript7.fvbot.data.repositories.TitleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.Title;
import io.github.hyscript7.fvbot.data.models.User;

@Service
public class TitleService {

    private final UserService userService;

    private final TitleRepository titleRepository;

    private final CharacterRepository characterRepository;

    TitleService(CharacterRepository characterRepository,
            TitleRepository titleRepository, UserService userService) {
        this.characterRepository = characterRepository;
        this.titleRepository = titleRepository;
        this.userService = userService;
    }

    public Optional<Title> getTitleById(Long id) {
        return titleRepository.findById(id);
    }

    public List<Title> getCharacterTitles(Character character) {
        return character.getTitles();
    }

    public List<Title> getUserCharacterTitles(User user) {
        List<Character> characters = characterRepository.findByUser(user);
        List<Title> titles = new ArrayList<>();
        for (Character character : characters) {
            titles.add(character.getTitle());
        }
        return titles;
    }

    public List<Title> getUserTitles(User user) {
        return user.getTitles();
    }

    public List<Title> getAllTitles() {
        return titleRepository.findAll();
    }

    public Title createTitle(String prefix, String suffix) {
        Title title = Title.builder().prefix(prefix).suffix(suffix).build();
        return titleRepository.save(title);
    }

    public void deleteTitle(Title title) {
        titleRepository.delete(title);
    }

    public boolean userHasTitle(Title title, User user) {
        Optional<Character> character = userService.getSelectedCharacter(user);
        return getUserTitles(user).contains(title) || character.isPresent() && getCharacterTitles(character.get()).contains(title);
    }

    public void grantTitle(User user, Title title) {
        User updatedUser = user;
        updatedUser.getTitles().add(title);
        userService.updateUser(updatedUser);
    }

    public void grantTitle(Character character, Title title) {
        Character updatedCharacter = character;
        updatedCharacter.getTitles().add(title);
        characterRepository.save(updatedCharacter);
    }

    public void revokeTitle(User user, Title title) {
        User updatedUser = user;
        updatedUser.getTitles().remove(title);
        userService.updateUser(updatedUser);
    }

    public void revokeTitle(Character character, Title title) {
        Character updatedCharacter = character;
        updatedCharacter.getTitles().remove(title);
        characterRepository.save(updatedCharacter);
    }

}
