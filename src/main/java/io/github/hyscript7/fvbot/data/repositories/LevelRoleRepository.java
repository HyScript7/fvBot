package io.github.hyscript7.fvbot.data.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import io.github.hyscript7.fvbot.data.models.LevelRole;

public interface LevelRoleRepository extends Repository<LevelRole, Long> {
    Optional<LevelRole> findByGuildIdAndRoleId(Long guildId, Long roleId);

    List<LevelRole> findByGuildIdAndLevelThresholdBetween(Long guildId, Integer start, Integer end);

    List<LevelRole> findAllByGuildId(Long guildId);

    void deleteByGuildId(Long guildId);

    void deleteByGuildIdAndLevelThresholdBetween(Long guildId, Integer start, Integer end);
}
