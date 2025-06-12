package io.github.hyscript7.fvbot.data.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "LevelRole", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "role_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // These IDs we get from discord, and we don't have copies of them in the database, because why would we?
    // At some point when guild settings become a thing, this could be an FK towards Guild(Settings), but atm we don't have that.
    private Long guildId;
    private Long roleId;
    private Integer levelThreshold;
    private boolean removeLowerRoles;
}
