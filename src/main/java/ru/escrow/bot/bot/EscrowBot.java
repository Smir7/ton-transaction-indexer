package ru.escrow.bot.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.escrow.bot.controller.UpdateRouter;

@Component
public class EscrowBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String token;
    private final TelegramClient telegramClient;
    private final UpdateRouter updateRouter;

    public EscrowBot(@Value("${bot.token}") String token,
                     UpdateRouter updateRouter,
                     TelegramClient telegramClient) {
        this.token = token;
        this.updateRouter = updateRouter;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        updateRouter.route(update, telegramClient);
    }
}
