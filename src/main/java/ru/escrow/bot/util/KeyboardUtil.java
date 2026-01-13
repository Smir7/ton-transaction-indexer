package ru.escrow.bot.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardUtil {

    /**
     * Генерирует клавиатуру для роли Покупателя
     */
    public static ReplyKeyboardMarkup getBuyerKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первая строка кнопок
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/deal");
        row1.add("/my_orders");
        keyboard.add(row1);

        // Вторая строка кнопок.
        // Кнопка  /set_seller/dev добавлена для демонстрации через один аккаунт телеграмм, при реализации убрать
        KeyboardRow row2 = new KeyboardRow();
        row2.add("/help");
        row2.add("/set_seller/dev");// для тестирования/ при реализации убрать
        keyboard.add(row2);


        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true) // Клавиатура подстраивается под размер экрана
                .selective(true) // Отображается только для тех, кто отправил сообщение
                .build();
    }

    /**
     * Генерирует клавиатуру для роли Продавца/Исполнителя
     */
    public static ReplyKeyboardMarkup getSellerKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первая строка кнопок
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/my_orders");
        row1.add("/disputes");
        keyboard.add(row1);

        // Вторая строка кнопок
        // /set_buyer/dev добавлена для демонстрации через один аккаунт телеграмм, при реализации убрать
        KeyboardRow row2 = new KeyboardRow();
        row2.add("/help");
        row2.add("/set_buyer/dev"); // для тестирования/  при реализации убрать
        keyboard.add(row2);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .selective(true)
                .build();
    }

    /**
     * Генерирует клавиатуру для роли Администратора, используя Lombok Builder.
     */
    public static ReplyKeyboardMarkup getAdminKeyboard() {
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Единственная строка кнопок для админа
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/disputes");
        keyboard.add(row1);


        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .selective(true)
                .build();
    }
}
