package ru.escrow.bot.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class MessageUtil {

    public static void send(TelegramClient client, Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void sendWithUrl(TelegramClient client, Long chatId, String text, String buttonText, String url) {
        InlineKeyboardButton urlButton = InlineKeyboardButton.builder()
                .text(buttonText)
                .url(url)
                .build();


        InlineKeyboardRow row = new InlineKeyboardRow(Collections.singletonList(urlButton));

        InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
                .keyboard(Collections.singletonList(row))
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboardMarkup)
                .build();
        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void sendWithKeyboard(TelegramClient client, Long chatId, String text, ReplyKeyboard keyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(keyboard) // Устанавливаем клавиатуру
                .build();
        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения с клавиатурой: " + e.getMessage());
        }
    }

    public static void sendWithButtons(TelegramClient client, Long chatId, String text, List<List<InlineKeyboardButton>> buttons) {


        List<InlineKeyboardRow> keyboardRows = buttons.stream()
                .map(InlineKeyboardRow::new)
                .collect(Collectors.toList());

        InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboardMarkup)
                .build();
        try {
            client.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    /**
     * Отправляет сообщение с кнопкой, которая вызывает callback (действие в боте)
     *
     * @param client       TelegramClient
     * @param chatId       ID чата
     * @param text         Текст сообщения
     * @param buttonText   Текст на кнопке
     * @param callbackData Данные, которые придут боту при нажатии (например, "PAY_DEAL_5")
     */
    public static void sendWithCallback(TelegramClient client, Long chatId, String text, String buttonText, String callbackData) {
        var button = InlineKeyboardButton.builder()
                .text(buttonText)
                .callbackData(callbackData)
                .build();

        var row = new InlineKeyboardRow(button);

        var keyboard = InlineKeyboardMarkup.builder()
                .keyboard(Collections.singletonList(row))
                .build();

        var msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();

        try {
            client.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static void sendWithArbitrationButtons(TelegramClient client, Long chatId, String text, Long dealId) {
        // Callback data будет выглядеть так: "RESOLVE_SELLER_5" или "RESOLVE_BUYER_5"

        var buttonSeller = InlineKeyboardButton.builder()
                .text(" Продавцу")
                .callbackData("RESOLVE_SELLER_" + dealId)
                .build();

        var buttonBuyer = InlineKeyboardButton.builder()
                .text(" Покупателю")
                .callbackData("RESOLVE_BUYER_" + dealId)
                .build();

        var row = new InlineKeyboardRow(Arrays.asList(buttonSeller, buttonBuyer));

        var keyboard = InlineKeyboardMarkup.builder()
                .keyboard(Collections.singletonList(row))
                .build();

        var msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboard)
                .build();

        try {
            client.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}


