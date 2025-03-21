package io.github.hyscript7.fvbot.data.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import io.github.hyscript7.fvbot.data.models.Title;

public interface TitleRepository extends Repository<Title, Long> {
    Title save(Title title);

    void delete(Title title);

    Optional<Title> findByPrefixAndSuffix(String prefix, String suffix);

    List<Title> findAll();

    List<Title> findByPrefix(String prefix);

    List<Title> findBySuffix(String suffix);
}
