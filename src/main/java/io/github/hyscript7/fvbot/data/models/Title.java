package io.github.hyscript7.fvbot.data.models;

import java.util.List;

import org.springframework.lang.Nullable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Title", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "innateName" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Title {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = true)
    private @Nullable String prefix;
    @Column(nullable = true)
    private @Nullable String suffix;

    @ManyToMany(mappedBy = "titles")
    private List<User> users;

    @ManyToMany(mappedBy = "titles")
    private List<Character> characters;
}
