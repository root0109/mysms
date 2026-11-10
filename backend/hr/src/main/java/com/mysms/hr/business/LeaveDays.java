package com.mysms.hr.business;

import java.math.BigDecimal;

/** How leave days are written in answers and messages: plain decimals without trailing zeros ("3", "0.5"). */
final class LeaveDays {

    private LeaveDays() {
    }

    static String plain(BigDecimal days) {
        BigDecimal stripped = days.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }
}
