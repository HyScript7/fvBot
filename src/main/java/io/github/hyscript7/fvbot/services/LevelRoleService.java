package io.github.hyscript7.fvbot.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import io.github.hyscript7.fvbot.data.models.Character;
import io.github.hyscript7.fvbot.data.models.LevelRole;
import io.github.hyscript7.fvbot.data.models.User;
import io.github.hyscript7.fvbot.data.repositories.LevelRoleRepository;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

@Service
public class LevelRoleService {
    private static final int PRESTIGE_LEVEL_REQUIREMENT = 50;

    private final UserService userService;

    private final LevelRoleRepository levelRoleRepository;

    LevelRoleService(LevelRoleRepository levelRoleRepository, UserService userService) {
        this.levelRoleRepository = levelRoleRepository;
        this.userService = userService;
    }

    public List<Role> getLevelRoles(Member member) {
        User user = userService.getOrCreateUser(member.getUser());
        Optional<Character> character = Optional.ofNullable(user.getCurrentCharacter());

        if (character.isEmpty())
            return List.of();

        int level = character.get().getLevel() + character.get().getResurrection() * PRESTIGE_LEVEL_REQUIREMENT;
        List<LevelRole> levelRoles = levelRoleRepository.findByGuildIdAndLevelThresholdBetween(
                member.getGuild().getIdLong(), 0, level + 1);

        if (levelRoles.isEmpty())
            return List.of();

        levelRoles.sort(Comparator.comparingInt(LevelRole::getLevelThreshold));

        List<LevelRole> reversed = levelRoles.reversed();
        List<Role> roles = new ArrayList<>();

        for (var levelRole : reversed) {
            var role = member.getGuild().getRoleById(levelRole.getRoleId());
            if (role != null)
                roles.add(role);
            if (levelRole.isRemoveLowerRoles())
                break;
        }

        return roles;
    }

    public List<Role> getAllLevelRoles(Guild guild) {
        // Holy mother of one liners
        return levelRoleRepository.findAllByGuildId(guild.getIdLong()).stream()
                .map(lr -> guild.getRoleById(lr.getRoleId())).filter(Objects::nonNull).toList();
    }

}
