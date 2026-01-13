 #### Описание проекта

Escrow Telegram Bot — это Telegram-бот для управления сделками с использованием гаранта (escrow) на блокчейне TON.
Бот обеспечивает безопасное взаимодействие между покупателями и продавцами, 
а также поддерживает механизм арбитража через администраторов.

 ### Основные возможности

- Создание сделок с указанием суммы в TON

- Автоматическое начисление комиссии 10% от суммы сделки

### Ролевая система:

    Покупатель

    Продавец

    Администратор

    Автоматическая проверка платежей через TON API

    Система арбитража для разрешения споров

    Динамические клавиатуры для каждой роли

    Конечный автомат (FSM) для управления состояниями сделок

 ### Технологический стек

    Java 17

    Spring Boot 3.2.3

    Telegram Bot API

    telegrambots-springboot-longpolling-starter 7.10.0

    H2 Database (встроенная, файловая)

    Spring Data JPA

    Lombok

    TON4J — работа с блокчейном TON
 
### Архитектура проекта
#### Основные пакеты

    ru.escrow.bot.bot — основной класс Telegram-бота

    ru.escrow.bot.config — конфигурация приложения и бота

    ru.escrow.bot.controller — маршрутизация обновлений Telegram

    ru.escrow.bot.domain — доменные сущности и enum’ы

    ru.escrow.bot.repository — JPA-репозитории

    ru.escrow.bot.service — бизнес-логика

    ru.escrow.bot.util — вспомогательные утилиты

###  Сущности
#### User
@Entity
@Table(name = "bot_users")
public class User {

    @Id
    private Long telegramId;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String currentDialogState;
}

#### Deal
@Entity
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User buyer;

    @ManyToOne
    private User seller;

    private Double amount;
    private Double commission; // 10%

    @Enumerated(EnumType.STRING)
    private DealState state;
}

###  Состояния сделок (DealState)

    CREATED — создана

    WAITING_PAYMENT — ожидает оплату

    PAID — оплачена

    IN_PROGRESS — исполнитель выполняет

    COMPLETED — успешно завершена

    DISPUTE — спор

    CANCELLED — отменена до оплаты

### Настройка и запуск
#### Требования

    Java 17

    Maven

    Telegram Bot Token

    TON-кошелек

    API-ключ TON

### Конфигурация

- Создайте файл application.yml:

        bot:
            token: YOUR_BOT_TOKEN
            username: YOUR_BOT_USERNAME

        server:
            port: 8081

        spring:
            datasource:
                url: jdbc:h2:file:./data/db
                driver-class-name: org.h2.Driver
        jpa:
            hibernate:
                ddl-auto: update
            database-platform: org.hibernate.dialect.H2Dialect

        ton:
            api:
                key: YOUR_TON_API_KEY
            wallet: YOUR_TON_WALLET_ADDRESS

## Сборка и запуск
### Сборка проекта
     mvn clean package

### Запуск приложения
    java -jar target/ton-escrow-telegram-bot-1.0.0.jar

### Работа с ботом
Основные команды
Для всех пользователей

    /start — приветственное сообщение

    /help — справка по боту

Для покупателей

    /deal — создать сделку

    /my_orders — мои заказы

    /pay — оплатить сделку

Для продавцов

    /my_orders — мои заказы

    /disputes — активные споры

Для администраторов

    /disputes — управление спорами

### Процесс сделки

#### Создание
Покупатель вводит 
    /deal
затем сумму в TON

#### Принятие
Продавец принимает сделку через 
/accept_{id}

#### Оплата
Покупатель оплачивает сделку командой 
/pay

#### Выполнение
Продавец завершает сделку через 
/done_{id}

#### Завершение
Сделка переходит в статус COMPLETED

#### Арбитраж

При возникновении спора любой участник может инициировать арбитраж:
/dispute_{id}


Администратор получает уведомление и может:

- принять решение в пользу продавца

- принять решение в пользу покупателя

### Особенности реализации
    Конечный автомат (FSM)

    FSM реализован в FSMService и контролирует допустимые переходы между состояниями сделок.

    Проверка платежей

    Сервис TonTransactionChecker проверяет входящие транзакции на TON-кошельке каждые 10 секунд.

    Динамические клавиатуры

    Класс KeyboardUtil формирует клавиатуры в зависимости от роли пользователя.

### Безопасность

    Все токены и ключи хранятся в конфигурации

    Проверка прав доступа по ролям

    Валидация переходов состояний сделок

### Структура проекта

src/main/java/ru/escrow/bot/
├── Application.java
│
├── bot/
│   │
│   └── EscrowBot.java
│
├── config/
│   │
│   ├── BotConfig.java
│   │
│   └── BotCommandsInitializer.java
│
├── controller/
│   │
│   └── UpdateRouter.java
├── domain/
│   │
│   ├── Deal.java
│   │
│   ├── DealState.java
│   │
│   ├── Role.java
│   │
│   └── User.java
├── repository/
│   │
│   ├── DealRepository.java
│   │
│   └── UserRepository.java
├── service/
│   │
│   ├── ArbitrationService.java
│   │
│   ├── DealService.java
│   │
│   ├── FSMService.java
│   │
│   ├── TonPaymentService.java
│   │
│   └── TonTransactionChecker.java
└── util/
├── KeyboardUtil.java
│   
└── MessageUtil.java

### Тестирование

Для тестирования без реальных платежей рекомендуется использовать:

TON testnet 
- запрос бесплатных TON разработчика 
    для тестирования работоспособьности платежной системы

Заглушки или mock-реализации TON API

### Лицензия

Проект распространяется под проприетарной лицензией.
Конфиденциальные данные (токены, ключи) удалены из публичной версии.

###

По вопросам и предложениям обращайтесь к разработчикам проекта.

## Важно:
    - В данный момент реализованы дополнительные возможности: 
        - смена ролей для тестирования
  
### Запуск в prodаct с такими настройками не предполагается

### Инструкция по переходу по перенастройке
    - отключение перехода ролей для заказчика и пользователя

Автор проекта
(Гончарова Ольга)