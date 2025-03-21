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
    @ManyToOne
    @JoinColumn(name = "title_id", nullable = true)
    private @Nullable Title title;
    private Integer level;
    private Double experience;

    @ManyToMany
    @JoinTable(name = "CharacterTitleGrant", joinColumns = @JoinColumn(name = "character_id"), inverseJoinColumns = @JoinColumn(name = "title_id"))
    private List<Title> titles;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
