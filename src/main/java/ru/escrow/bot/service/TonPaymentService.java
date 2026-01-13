package ru.escrow.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TonPaymentService {

    @Value("${ton.wallet}")
    private String escrowWalletAddress;

    public String createTonInvoiceLink(Long dealId, double amount) {
        // Уникальный комментарий для поиска транзакции
        String comment = "deal_" + dealId;

        // Сумма в нанотонах
        long nanoAmount = (long) (amount * 1_000_000_000);

        // Формируем ссылку, автоматически открывающую кошелек
        return String.format(
                "ton://transfer/%s?amount=%d&text=%s",
                escrowWalletAddress,
                nanoAmount,
                URLEncoder.encode(comment, StandardCharsets.UTF_8)
        );
    }
}
