package io.github.hyscript7.fvbot.data.models;

import java.util.List;

import org.springframework.lang.Nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Member", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "discord_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long discordId;
    private String username;
    @OneToOne
    @JoinColumn(name = "current_character_id", nullable = true)
    private @Nullable Character currentCharacter;

    @ManyToMany
    @JoinTable(name = "UserTitleGrant", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "title_id"))
    private List<Title> titles;

    @OneToMany
    private List<Character> characters;
}
