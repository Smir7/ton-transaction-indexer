package ru.escrow.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.escrow.bot.domain.Deal;
import ru.escrow.bot.domain.DealState;
import ru.escrow.bot.domain.User;
import java.util.List;
import java.util.Optional;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {

    List<Deal> findAllBySellerAndState(User seller, DealState state);

    List<Deal> findAllByBuyer(User buyer);

    List<Deal> findAllByState(DealState state);

    // Найти последнюю сделку пользователя в статусе CREATED
    Optional<Deal> findFirstByBuyerAndStateOrderByIdDesc(User buyer, DealState state);

}
