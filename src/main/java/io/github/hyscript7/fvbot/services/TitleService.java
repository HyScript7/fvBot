package io.github.hyscript7.fvbot.services;

import io.github.hyscript7.fvbot.data.repositories.CharacterRepository;
import io.github.hyscript7.fvbot.data.repositories.TitleRepository;
import io.github.hyscript7.fvbot.data.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.Title;
import io.github.hyscript7.fvbot.data.models.User;

@Service
public class TitleService {

    private final TitleRepository titleRepository;

    private final CharacterRepository characterRepository;

    TitleService(CharacterRepository characterRepository,
            TitleRepository titleRepository) {
        this.characterRepository = characterRepository;
        this.titleRepository = titleRepository;
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

}
