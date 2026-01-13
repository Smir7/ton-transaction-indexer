package ru.escrow.bot.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BotCommandsInitializer {

    private final TelegramClient telegramClient;

    @PostConstruct
    public void initCommands() {
        // базовые команды, видимые в меню слева от поля ввода
        List<BotCommand> commands = Arrays.asList(
                new BotCommand("/start", " Приветственное сообщение"),
                new BotCommand("/help", " Справка по боту")
                // остальные кнопки в динамических клавиатурах по ролям
        );


        SetMyCommands setMyCommands = new SetMyCommands(commands);
        setMyCommands.setScope(new BotCommandScopeDefault());
        setMyCommands.setLanguageCode(null);

        try {
            telegramClient.execute(setMyCommands);
            System.out.println(" Команды бота успешно инициализированы через API.");
        } catch (TelegramApiException e) {
            System.err.println(" Ошибка при инициализации команд бота: " + e.getMessage());
        }
    }
}
