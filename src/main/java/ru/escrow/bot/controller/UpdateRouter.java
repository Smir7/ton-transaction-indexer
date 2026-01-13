package ru.escrow.bot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import ru.escrow.bot.util.KeyboardUtil;
import ru.escrow.bot.domain.Deal;
import ru.escrow.bot.domain.DealState;
import ru.escrow.bot.domain.Role;
import ru.escrow.bot.domain.User;
import ru.escrow.bot.repository.DealRepository;
import ru.escrow.bot.repository.UserRepository;
import ru.escrow.bot.service.ArbitrationService;
import ru.escrow.bot.service.DealService;
import ru.escrow.bot.service.TonPaymentService;
import ru.escrow.bot.util.MessageUtil;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateRouter {
    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final DealService dealService;
    private final TonPaymentService tonPaymentService;
    private final ArbitrationService arbitrationService;

    public void route(Update update, TelegramClient client) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(client, update.getCallbackQuery());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        User user = userRepository.findById(chatId).orElseGet(() -> registerNewUser(chatId));

        sendRoleSpecificMenu(client, user, text, chatId);
    }

    private void sendRoleSpecificMenu(TelegramClient client, User user, String text, Long chatId) {
        ReplyKeyboard keyboard;
        String welcomeMessage = "Здравствуйте! Выберите действие:";

        if (user.getRole() == Role.ADMIN) {
            keyboard = KeyboardUtil.getAdminKeyboard();
        } else if (user.getRole() == Role.SELLER) {
            keyboard = KeyboardUtil.getSellerKeyboard();
            if ("WAITING_FOR_AMOUNT".equals(user.getCurrentDialogState())) {
                handleAmountInput(client, user, text);
                return;
            }
        } else {
            keyboard = KeyboardUtil.getBuyerKeyboard();
            if ("WAITING_FOR_AMOUNT".equals(user.getCurrentDialogState())) {
                handleAmountInput(client, user, text);
                return;
            }
        }

        // STATELESS логика (команды)
        switch (text) {
            case "/start": sendWelcome(client, chatId); break; // Использует chatId
            case "/deal": startDealCreation(client, user); break;
            case "/pay": handlePayment(client, user); break;
            case "/my_orders": handleSellerOrders(client, user); break;
            case "/disputes": handleAdminDisputes(client, user); break;
            case "/set_seller":
                user.setRole(Role.SELLER);
                userRepository.save(user);
                MessageUtil.sendWithKeyboard(client, chatId, " Режим продавца активирован. Обновите меню или введите /start", KeyboardUtil.getSellerKeyboard());
                break;
            case "/set_buyer":
                user.setRole(Role.BUYER);
                userRepository.save(user);
                MessageUtil.sendWithKeyboard(client, chatId, " Режим покупателя активирован. Обновите меню или введите /start", KeyboardUtil.getBuyerKeyboard());
                break;
            default:
                // Обработка команд типа /accept_5 или /done_5 ЗДЕСЬ ПАРСИМ ID
                if (text.startsWith("/accept_")) {
                    try {
                        Long dealId = Long.parseLong(text.replace("/accept_", ""));
                        handleAcceptDeal(client, user, dealId);
                    } catch (NumberFormatException e) {
                        MessageUtil.send(client, chatId, " Некорректный ID сделки.");
                    }
                }
                else if (text.startsWith("/done_")) {
                    try {
                        Long dealId = Long.parseLong(text.replace("/done_", ""));
                        handleCompleteDeal(client, user, dealId);
                    } catch (NumberFormatException e) {
                        MessageUtil.send(client, chatId, " Некорректный ID сделки.");
                    }
                }
                else if (text.startsWith("/dispute_")) {
                    try {
                        Long dealId = Long.parseLong(text.replace("/dispute_", ""));
                        arbitrationService.startDispute(client,dealId, user);
                    } catch (NumberFormatException e) {
                        MessageUtil.send(client, chatId, " Некорректный ID сделки.");
                    }
                } else {
                    MessageUtil.sendWithKeyboard(client, chatId, welcomeMessage, keyboard);
                }
                break;
        }
    }

    private void handleCallbackQuery(TelegramClient client, CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        User user = userRepository.findById(chatId).orElseGet(() -> registerNewUser(chatId));
        if (data.startsWith("RESOLVE_SELLER_") && user.getRole() == Role.ADMIN) {
            Long dealId = Long.parseLong(data.substring("RESOLVE_SELLER_".length()));
            arbitrationService.resolveToSeller(client, dealId);
            MessageUtil.send(client, chatId, " Решение принято: средства Продавцу.");
        }
        else if (data.startsWith("RESOLVE_BUYER_") && user.getRole() == Role.ADMIN) {
            Long dealId = Long.parseLong(data.substring("RESOLVE_BUYER_".length()));
            arbitrationService.resolveToBuyer(client, dealId);
            MessageUtil.send(client, chatId, " Решение принято: возврат Покупателю.");
        }
        if (data.startsWith("ACCEPT_")) {
            try {
                Long dealId = Long.parseLong(data.substring(7));
                handleAcceptDeal(client, user, dealId);
            } catch (NumberFormatException e) {
                MessageUtil.send(client, chatId, " Некорректный ID сделки.");
            }
        } else if (data.startsWith("DONE_")) {
            try {
                Long dealId = Long.parseLong(data.substring(5));
                handleCompleteDeal(client, user, dealId);
            } catch (NumberFormatException e) {
                MessageUtil.send(client, chatId, " Некорректный ID сделки.");
            }
        } else if ("PAY_DEAL".equals(data)) {
            handlePayment(client, user);
        } else {
            MessageUtil.send(client, chatId, "Обработка нажатия кнопки: " + data);
        }
    }

    // --- ОБРАБОТКА КОМАНД ИЗ UX_FLOW
    private User registerNewUser(Long chatId) {
        User newUser = new User();
        newUser.setTelegramId(chatId);
        newUser.setRole(Role.BUYER);
        return userRepository.save(newUser);
    }

    private void sendWelcome(TelegramClient client, Long chatId) {
        User user = userRepository.findById(chatId).orElseGet(() -> registerNewUser(chatId));
        ReplyKeyboard keyboard;
        if (user.getRole() == Role.ADMIN) {
            keyboard = KeyboardUtil.getAdminKeyboard();
        } else if (user.getRole() == Role.SELLER) {
            keyboard = KeyboardUtil.getSellerKeyboard();
        } else {
            keyboard = KeyboardUtil.getBuyerKeyboard();
        }
        MessageUtil.sendWithKeyboard(client, chatId, " Добро пожаловать в Escrow Bot!", keyboard);
    }

    private void startDealCreation(TelegramClient client, User user) {
        user.setCurrentDialogState("WAITING_FOR_AMOUNT");
        userRepository.save(user);
        MessageUtil.send(client, user.getTelegramId(), " Введите сумму сделки в TON:");
    }

    private void handleAmountInput(TelegramClient client, User user, String text) {
        try {
            double amount = Double.parseDouble(text.replace(",", "."));
            Deal newDeal = dealService.createDeal(user, null, amount);
            user.setCurrentDialogState("WAITING_FOR_SELLER_DEALID_" + newDeal.getId());
            userRepository.save(user);
            String message = String.format(" Сделка создана на сумму %.2f TON.\\nКомиссия (10%%): %.2f TON.\\n\\n", amount, (amount * 0.1));
            MessageUtil.send(client, user.getTelegramId(), message + "Теперь попросите Исполнителя написать: /accept_" + newDeal.getId());
        } catch (NumberFormatException e) {
            MessageUtil.send(client, user.getTelegramId(), " Ошибка! Пожалуйста, введите число (например, 10.5):");
        }
    }

    private void handlePayment(TelegramClient client, User user) {
        Optional<Deal> dealOpt = dealRepository.findFirstByBuyerAndStateOrderByIdDesc(user, DealState.CREATED);
        if (dealOpt.isEmpty()) {
            MessageUtil.send(client, user.getTelegramId(), " У вас нет активных сделок для оплаты. Создайте новую через /deal");
            return;
        }
        var deal = dealOpt.get();
        if (deal.getSeller() == null) {
            MessageUtil.send(client, user.getTelegramId(), " Исполнитель еще не назначен. Попросите его принять сделку через /accept.");
            return;
        }
        Long dealId = deal.getId();
        double amount = deal.getAmount();
        String paymentLink = tonPaymentService.createTonInvoiceLink(dealId, amount);
        String message = String.format(" Оплата сделки #%d\\nСумма: %.2f TON\\n\\nНажмите на кнопку ниже для оплаты:", dealId, amount);
        MessageUtil.sendWithUrl(client, user.getTelegramId(), message, "Оплатить в TON", paymentLink);
    }

    private void handleSellerOrders(TelegramClient client, User user) {
        if (user.getRole() != Role.SELLER) return;
        List<Deal> orders = dealRepository.findAllBySellerAndState(user, DealState.PAID);
        if (orders.isEmpty()) {
            MessageUtil.send(client, user.getTelegramId(), " У вас нет активных заказов в работе.");
            return;
        }
        MessageUtil.send(client, user.getTelegramId(), " Ваши заказы в работе: " + orders.size() + " шт.");
    }

    private void handleAdminDisputes(TelegramClient client, User user) {
        if (user.getRole() != Role.ADMIN) return;
        List<Deal> disputes = arbitrationService.getActiveDisputes();
        if (disputes.isEmpty()) {
            MessageUtil.send(client, user.getTelegramId(), " Нет активных споров.");
            return;
        }
        MessageUtil.send(client, user.getTelegramId(), " Активные споры: " + disputes.size() + " шт.");
    }

    private void handleAcceptDeal(TelegramClient client, User user, Long dealId) {
        if (user.getRole() != Role.SELLER) {
            MessageUtil.send(client, user.getTelegramId(), " Вы не можете принять сделку. Смените роль на /set_seller.");
            return;
        }
        var dealOpt = dealRepository.findById(dealId);
        if (dealOpt.isEmpty() || dealOpt.get().getState() != DealState.CREATED) {
            MessageUtil.send(client, user.getTelegramId(), " Сделка не найдена, неактивна или уже принята.");
            return;
        }
        Deal deal = dealOpt.get();
        if (deal.getSeller() != null) {
            MessageUtil.send(client, user.getTelegramId(), " У этой сделки уже есть исполнитель.");
            return;
        }
        deal.setSeller(user);
        dealRepository.save(deal);
        MessageUtil.send(client, user.getTelegramId(), " Вы приняли сделку #" + deal.getId() + ". Ожидаем оплату от Заказчика.");
        MessageUtil.send(client, deal.getBuyer().getTelegramId(), " Ваш заказ принят исполнителем! Можете оплачивать через /pay");
    }

    private void handleCompleteDeal(TelegramClient client, User user, Long dealId) {
        if (user.getRole() != Role.SELLER) return;
        dealService.updateState(dealId, DealState.COMPLETED, client);
        MessageUtil.send(client, user.getTelegramId(), " Заказ #" + dealId + " отмечен как выполненный.");
    }
}
