package ru.escrow.bot.service;

import org.springframework.stereotype.Service;
import ru.escrow.bot.domain.DealState;

import java.util.Map;
import java.util.Set;

@Service
public class FSMService {

    private static final Map<DealState, Set<DealState>> TRANSITIONS = Map.of(
            DealState.CREATED, Set.of(
                    DealState.WAITING_PAYMENT,
                    DealState.CANCELLED
            ),
            DealState.WAITING_PAYMENT, Set.of(
                    DealState.PAID,
                    DealState.CANCELLED
            ),
            DealState.PAID, Set.of(
                    DealState.IN_PROGRESS
            ),
            DealState.IN_PROGRESS, Set.of(
                    DealState.COMPLETED,
                    DealState.DISPUTE
            ),
            DealState.DISPUTE, Set.of(
                    DealState.CANCELLED
            ),
            DealState.CANCELLED, Set.of(),
            DealState.COMPLETED, Set.of()
    );

    public DealState advance(DealState current, DealState next) {

        if (current == next) {
            return current;
        }

        Set<DealState> allowed = TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                    "Invalid transition: " + current + " -> " + next
            );
        }

        return next;
    }
}
