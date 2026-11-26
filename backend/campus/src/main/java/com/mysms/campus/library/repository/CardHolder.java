package com.mysms.campus.library.repository;

import java.util.UUID;

/** Whose library card a loan was made on: the card's institution and holder (a student or a staff member). */
public record CardHolder(UUID institutionId, UUID personId, UUID studentId, String holderType) {
}
