package io.github.hyscript7.fvbot.data.models;

import java.util.List;

import org.springframework.lang.Nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Character", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "innateName" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    private String innateName;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "title_id", nullable = true)
    private @Nullable Title title;
    private Integer resurrection;
    private Integer level;
    private Double experience;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "CharacterTitleGrant", joinColumns = @JoinColumn(name = "character_id"), inverseJoinColumns = @JoinColumn(name = "title_id"))
    private List<Title> titles;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFullNameWithTitle() {
        String prefix = title.getPrefix() == null ? "" : title.getPrefix();
        String suffix = title.getSuffix() == null ? "" : title.getSuffix();
        return prefix + firstName + " " + lastName + suffix;
    }

    /**
     * Generates a full name for discord nickname, which has a max length of 32.
     * If the full name with title is shorter than 32, returns the full name with
     * title.
     * If the full name without title is shorter than 32, returns the full name
     * without title.
     * Otherwise, returns the substring of the full name without title from index 0
     * to 32.
     * 
     * @return a string that can be used as discord nickname
     */
    public String discordFullName() {
        String withTitle = getFullNameWithTitle();
        String withoutTitle = getFullName();
        if (withTitle.length() <= 32) {
            return withTitle;
        }
        if (withoutTitle.length() <= 32) {
            return withoutTitle;
        }
        return withoutTitle.substring(0, 32);
    }
}
