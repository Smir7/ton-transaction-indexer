package ru.escrow.bot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User buyer;

    @ManyToOne
    private User seller;

    private Double amount;
    private Double commission; // 10%

    @Enumerated(EnumType.STRING)
    private DealState state;

    public Deal() {
        this.state = DealState.CREATED;
    }
}
