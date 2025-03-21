package io.github.hyscript7.fvbot.data.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.User;

public interface CharacterRepository extends Repository<Character, Long> {
    Character save(Character character);

    void delete(Character character);

    Optional<Character> findByInnateName(String innateName);

    List<Character> findByFirstNameAndLastName(String firstName, String lastName);

    List<Character> findByUser(User user);
}
