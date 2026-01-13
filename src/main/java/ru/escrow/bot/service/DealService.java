package ru.escrow.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.escrow.bot.domain.Deal;
import ru.escrow.bot.domain.DealState;
import ru.escrow.bot.domain.User;
import ru.escrow.bot.repository.DealRepository;
import ru.escrow.bot.util.MessageUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;
    private final FSMService fsmService;

    /**
     * Создание новой сделки (CREATED)
     */
    @Transactional
    public Deal createDeal(User buyer, User seller, Double amount) {
        Deal deal = new Deal();
        deal.setBuyer(buyer);
        deal.setSeller(seller);
        deal.setAmount(amount);
        // Расчет комиссии 10%
        deal.setCommission(amount * 0.10);
        deal.setState(DealState.CREATED);

        return dealRepository.save(deal);
    }



    @Transactional
    public void markDealAsPaid(Long dealId, TelegramClient client) {
        updateState(dealId, DealState.PAID, client);
        System.out.println("--- УВЕДОМЛЕНИЕ ИСПОЛНИТЕЛЮ --- Сделка #" + dealId + " оплачена.");
    }

    @Transactional
    public void updateState(Long dealId, DealState newState, TelegramClient client) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealId));

        DealState updatedState = fsmService.advance(deal.getState(), newState);
        deal.setState(updatedState);

        if (updatedState == DealState.PAID) {
            MessageUtil.send(client, deal.getSeller().getTelegramId(), " Заказ #" + dealId + " оплачен! Можете начинать.");
        }

        if (updatedState == DealState.COMPLETED) {
            // Уведомление Покупателю
            MessageUtil.send(client, deal.getBuyer().getTelegramId(), " Заказ #" + dealId + " выполнен исполнителем. Проверьте и подтвердите.");
        }

        dealRepository.save(deal);
    }

    /**
     * Получение заказов для Исполнителя (Seller)
     */
    public List<Deal> getSellerOrders(User seller) {
        return dealRepository.findAllBySellerAndState(seller, DealState.IN_PROGRESS);
    }

    /**
     * Получение сделок для Арбитража (Admin)
     */
    public List<Deal> getDisputedDeals() {
        return dealRepository.findAllByState(DealState.DISPUTE);
    }
}

