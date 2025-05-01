/**
 * Original implementation by:
 * Copyright (c) 2025 by Zack Guard (https://codepen.io/z8ff6efe39c/pen/GgVeXj)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Java implementation by HyScript7
 */

package io.github.hyscript7.fvbot.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import io.github.hyscript7.fvbot.data.repositories.CharacterRepository;

@Service
public class InnateNameService {
    private static final String VOWELS = "aeiouy";
    private static final String CONSONANTS = "bcdfghjklmnprstvwxz";

    private final Random random;
    private final CharacterRepository characterRepository;

    public InnateNameService(CharacterRepository characterRepository) {
        this.random = new Random();
        this.characterRepository = characterRepository;
    }

    /**
     * Generates a pseudo-random innate name composed of alternating consonants and
     * vowels.
     * 
     * @param minLength The minimum length of the generated name. Must be at least
     *                  4.
     * @param maxLength The maximum length of the generated name.
     * @return A string representing the generated innate name.
     */

    private String generate(int minLength, int maxLength) {
        minLength = Math.max(4, minLength);
        int length = random.nextInt(maxLength - minLength + 1) + minLength;
        int pairsCount = length / 2;
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < pairsCount; j++) {
            char vowel = VOWELS.charAt(random.nextInt(VOWELS.length()));
            char consonant;
            do {
                consonant = CONSONANTS.charAt(random.nextInt(CONSONANTS.length()));
            } while (j == 0 && consonant == 'x');
            sb.append(consonant).append(vowel);
        }
        return sb.substring(0, Math.min(length, sb.length()));
    }

    /**
     * Generates a list of unique pseudo-random innate names.
     *
     * @param n         The number of names to generate.
     * @param minLength The minimum length of each generated name. Must be at least
     *                  4.
     * @param maxLength The maximum length of each generated name.
     * @return A list of strings representing the generated unique innate names.
     */

    public List<String> generateMultipleInnateNames(int n, int minLength, int maxLength) {
        List<String> result = new ArrayList<>();
        while (result.size() < n) {
            String name = generate(minLength, maxLength);
            // Check if the name is already in use. We don't want to allow duplicate names.
            if (characterRepository.findByInnateName(name).isEmpty()) {
                result.add(name);
            }
        }
        return result;
    }

    /**
     * Generates a single unique pseudo-random innate name.
     * 
     * @param minLength The minimum length of the generated name. Must be at least
     *                  4.
     * @param maxLength The maximum length of the generated name.
     * @return A string representing the generated unique innate name.
     */
    public String generateInnateName(int minLength, int maxLength) {
        return generateMultipleInnateNames(1, minLength, maxLength).get(0);
    }
}
