package ru.escrow.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.escrow.bot.domain.Deal;
import ru.escrow.bot.domain.DealState;
import ru.escrow.bot.domain.Role;
import ru.escrow.bot.repository.DealRepository;
import ru.escrow.bot.repository.UserRepository;
import ru.escrow.bot.util.MessageUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArbitrationService {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final FSMService fsmService;

    public List<Deal> getActiveDisputes() {
        return dealRepository.findAllByState(DealState.DISPUTE);
    }

    /**
     * Инициирует спор по сделке и уведомляет администраторов.
     *
     * @param dealId ID сделки.
     * @param user   Пользователь, инициирующий спор.
     */

    @Transactional
    public void startDispute(TelegramClient client, Long dealId, ru.escrow.bot.domain.User user) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found"));

        if (deal.getState() != DealState.PAID && deal.getState() != DealState.IN_PROGRESS) {
            MessageUtil.send(client, user.getTelegramId(), " Спор можно открыть только по активной оплаченной сделке.");
            return;
        }

        if (deal.getState() == DealState.DISPUTE) {
            MessageUtil.send(client, user.getTelegramId(), " По этой сделке уже идет спор.");
            return;
        }

        fsmService.advance(deal.getState(), DealState.DISPUTE);
        dealRepository.save(deal);

        MessageUtil.send(client, user.getTelegramId(), " Спор по сделке #" + dealId + " передан администратору на рассмотрение.");

        Long otherUserId = user.equals(deal.getBuyer()) ? deal.getSeller().getTelegramId() : deal.getBuyer().getTelegramId();
        MessageUtil.send(client, otherUserId, " Пользователь " + user.getTelegramId() + " инициировал спор по сделке #" + dealId + ".");

        notifyAdminOfDispute(client, deal); // Передаем client дальше
    }

    @Transactional
    public void resolveToSeller(TelegramClient client, Long dealId) {
        resolveDispute(client, dealId, Role.SELLER);
    }

    @Transactional
    public void resolveToBuyer(TelegramClient client, Long dealId) {
        resolveDispute(client, dealId, Role.BUYER);
    }

    /**
     * Основной метод разрешения спора с отправкой уведомлений.
     */
    @Transactional
    public void resolveDispute(TelegramClient client, Long dealId, Role winnerRole) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found"));

        if (deal.getState() != DealState.DISPUTE) {
            throw new IllegalStateException("Can only resolve disputes");
        }

        DealState targetState;
        String messageForWinner;
        String messageForLoser;
        Long winnerChatId;
        Long loserChatId;

        if (winnerRole == Role.BUYER) {
            // Деньги возвращаются покупателю
            System.out.println("Выплачиваем " + deal.getAmount() + " TON покупателю " + deal.getBuyer().getTelegramId());
            targetState = DealState.CANCELLED;
            messageForWinner = String.format(" Администратор решил спор в вашу пользу. Средства (%.2f TON) возвращены на ваш счет.", deal.getAmount());
            messageForLoser = String.format(" Администратор решил спор не в вашу пользу по сделке #%d. Средства не получены.", dealId);
            winnerChatId = deal.getBuyer().getTelegramId();
            loserChatId = deal.getSeller().getTelegramId();
        } else if (winnerRole == Role.SELLER) {
            // Деньги (минус комиссия) выплачиваются продавцу
            double amountToSeller = deal.getAmount() - deal.getCommission();
            System.out.println("Выплачиваем " + amountToSeller + " TON продавцу " + deal.getSeller().getTelegramId());
            targetState = DealState.COMPLETED;
            messageForWinner = String.format(" Администратор решил спор в вашу пользу. Средства (%.2f TON за вычетом комиссии) отправлены вам.", amountToSeller);
            messageForLoser = String.format(" Администратор решил спор не в вашу пользу по сделке #%d. Средства не возвращены.", dealId);
            winnerChatId = deal.getSeller().getTelegramId();
            loserChatId = deal.getBuyer().getTelegramId();
        } else {
            throw new IllegalArgumentException("Invalid winner role");
        }

        // Меняем статус через FSM и сохраняем
        fsmService.advance(deal.getState(), targetState);
        dealRepository.save(deal);

        // !!! ОТПРАВКА ФИНАЛЬНЫХ УВЕДОМЛЕНИЙ
        MessageUtil.send(client, winnerChatId, messageForWinner);
        MessageUtil.send(client, loserChatId, messageForLoser);
    }


    private void notifyAdminOfDispute(TelegramClient client, Deal deal) {
        List<ru.escrow.bot.domain.User> admins = userRepository.findAllByRole(Role.ADMIN);
        String message = String.format("️ Новый спор по сделке #%d.\\nЗаказчик: %s\\nИсполнитель: %s\\nСумма: %.2f TON",
                deal.getId(), deal.getBuyer().getTelegramId(), deal.getSeller().getTelegramId(), deal.getAmount());

        for (ru.escrow.bot.domain.User admin : admins) {
            // Используем метод из MessageUtil с кнопками арбитража
            MessageUtil.sendWithArbitrationButtons(client, admin.getTelegramId(), message, deal.getId());
        }

    }
}
