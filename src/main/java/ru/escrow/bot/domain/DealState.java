package ru.escrow.bot.domain;

public enum DealState {
    CREATED,              // создана
    WAITING_PAYMENT,      // ожидает оплату
    PAID,                 // оплачена
    IN_PROGRESS,          // исполнитель выполняет
    COMPLETED,            // успешно завершена
    DISPUTE,              // спор
    CANCELLED              // отменена до оплаты
}
