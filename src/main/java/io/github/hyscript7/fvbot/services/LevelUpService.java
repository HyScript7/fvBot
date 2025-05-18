package io.github.hyscript7.fvbot.services;

import org.springframework.stereotype.Service;

@Service
public class LevelUpService {
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
     * Calculates how many level ups we can perform given the character's current experience,
     * and updates the character's level and experience accordingly.

     * If the character is already at level 50, this method does nothing.
     * 
     * @see io.github.hyscript7.fvbot.services.CharacterService#updateCharacter(io.github.hyscript7.fvbot.data.models.Character)
     * You must use the CharacterService to save the character's new level and experience after calling this method.
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
        // Logic for calculating how many level ups we can perform and how much experience is required
        do {
            levelUps += 1;
            totalReq += expRequirementForLevel(currentLevel + levelUps);
        } while (character.getExperience() - totalReq >= expRequirementForLevel(currentLevel + levelUps + 1));
        character.setLevel(Math.min(currentLevel + levelUps, 50));
        character.setExperience(character.getExperience() - totalReq);
    }
}
