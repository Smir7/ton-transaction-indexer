package ru.escrow.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.escrow.bot.domain.Deal;
import ru.escrow.bot.domain.DealState;
import ru.escrow.bot.repository.DealRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TonTransactionChecker {

    private final DealRepository dealRepository;
    private final DealService dealService;

    // 1. Внедряем TelegramClient для отправки уведомлений
    private final TelegramClient telegramClient;

    private final RestClient restClient = RestClient.create();

    @Value("${ton.api.key}")
    private String tonApiKey;

    @Value("${ton.wallet}")
    private String escrowWalletAddress;

    private static final String TON_CENTER_URL = "testnet.toncenter.com";

    @Scheduled(fixedRate = 10000)
    public void checkPendingPayments() {
        List<Deal> pendingDeals = dealRepository.findAllByState(DealState.WAITING_PAYMENT);
        if (pendingDeals.isEmpty()) return;

        try {
            String url = String.format("%s?address=%s&limit=10", TON_CENTER_URL, escrowWalletAddress);

            JsonNode response = restClient.get()
                    .uri(url)
                    .header("X-API-Key", tonApiKey)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("result")) {
                for (JsonNode tx : response.get("result")) {
                    processTransaction(tx, pendingDeals);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка TON API: {}", e.getMessage());
        }
    }

    private void processTransaction(JsonNode tx, List<Deal> pendingDeals) {
        JsonNode inMsg = tx.get("in_msg");
        if (inMsg == null || !inMsg.has("message")) return;

        String comment = inMsg.get("message").asText();

        pendingDeals.stream()
                .filter(deal -> ("pay_deal_" + deal.getId()).equals(comment))
                .findFirst()
                .ifPresent(this::confirmPayment);
    }

    // 2. Метод подтверждения и уведомления
    private void confirmPayment(Deal deal) {
        log.info("✅ Платеж подтвержден для сделки #{}", deal.getId());

        // 1. Обновляем статус сделки в БД через сервис
        dealService.updateState(deal.getId(), DealState.PAID, null);

        // 2. Уведомляем Покупателя
        if (deal.getBuyer() != null) {
            sendTelegramNotification(
                    deal.getBuyer().getTelegramId(), // Используем  telegramId
                    "💰 Оплата получена! Ваш заказ #" + deal.getId() + " переведен в работу."
            );
        }

        // 3. Уведомляем Продавца
        if (deal.getSeller() != null) {
            sendTelegramNotification(
                    deal.getSeller().getTelegramId(), // Используем  telegramId
                    "📢 Заказ #" + deal.getId() + " оплачен! Можете приступать к выполнению."
            );
        }
    }

    private void sendTelegramNotification(Long chatId, String text) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString()) // Переводим Long в String для Telegram
                    .text(text)
                    .build();
            telegramClient.execute(message);
        } catch (Exception e) {
            log.error("Ошибка при отправке уведомления в TG: {}", e.getMessage());
        }
    }
}
