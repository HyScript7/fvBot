package io.github.hyscript7.fvbot.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

@Service
@Slf4j
public class LevelUpService {

    private final CharacterService characterService;

    private final LevelRoleService levelRoleService;

    LevelUpService(CharacterService characterService, LevelRoleService levelRoleService) {
        this.characterService = characterService;
        this.levelRoleService = levelRoleService;
    }

    /**
     * Calculates the experience requirement for a given level.
     *
     * @param level the level
     * @return the experience requirement for the given level
     */
    public double expRequirementForLevel(int level) {
        return Math.round(100 * Math.log(level + 1d));
    }

    /**
     * Calculates how many level ups we can perform given the character's current
     * experience,
     * and updates the character's level and experience accordingly.
     * 
     * If the character is already at level 50, this method does nothing.
     * 
     * @see io.github.hyscript7.fvbot.services.CharacterService#updateCharacter(io.github.hyscript7.fvbot.data.models.Character)
     *      You must use the CharacterService to save the character's new level and
     *      experience after calling this method.
     * 
     * @param character The character to perform level ups on.
     */
    public void performEligibleLevelUps(io.github.hyscript7.fvbot.data.models.Character character) {
        int currentLevel = character.getLevel();
        if (currentLevel >= 50) {
            return;
        }
        double totalReq = 0;
        int levelUps = 0;
        // Logic for calculating how many level ups we can perform and how much
        // experience is required
        do {
            levelUps += 1;
            totalReq += expRequirementForLevel(currentLevel + levelUps);
        } while (character.getExperience() - totalReq >= expRequirementForLevel(currentLevel + levelUps + 1));
        if (character.getExperience() - totalReq < 0.0d) {
            return;
        }
        character.setLevel(Math.min(currentLevel + levelUps, 50));
        character.setExperience(character.getExperience() - totalReq);
    }

    public void runNecessaryUpdates(Member member, io.github.hyscript7.fvbot.data.models.Character character) {
        int levelBeforeUpdate = character.getLevel();
        performEligibleLevelUps(character);
        characterService.updateCharacter(character);
        if (levelBeforeUpdate != character.getLevel()) {
            log.info("Character {} has leveled up to level {}!", character.getFullNameWithTitle(),
                    character.getLevel());
            // Update roles
            List<Role> levelRoles = levelRoleService.getLevelRoles(member); // The roles the user is supposed to have
            List<Role> allLevelRoles = levelRoleService.getAllLevelRoles(member.getGuild()); // All level roles
            List<Role> nonLevelMemberRoles = member.getRoles().stream()
                    .filter(r -> !allLevelRoles.contains(r) || (allLevelRoles.contains(r) && levelRoles.contains(r)))
                    .toList(); // The member's new (complete) role list
            List<Role> newMemberRoles = new ArrayList<>();
            newMemberRoles.addAll(nonLevelMemberRoles);
            newMemberRoles.addAll(levelRoles);
            member.getGuild().modifyMemberRoles(member, newMemberRoles).queue();
        }
    }
}
