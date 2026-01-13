package ru.escrow.bot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "bot_users")
public class User {

    @Id
    private Long telegramId;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String currentDialogState;
}
