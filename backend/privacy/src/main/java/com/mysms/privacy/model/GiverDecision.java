package com.mysms.privacy.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One giver's current decision about a subject and a purpose, as the effective consent rule weighs it: a consent
 * record that is neither withdrawn nor superseded. Whether it has lapsed or is verified is for the rule to judge.
 *
 * @param giverPersonId the person who decided
 * @param granted       true for a grant, false for a refusal
 * @param scope         the width (or tenant) decided at
 * @param validFrom     when the decision was recorded
 * @param validUntil    the last day it counts; null = until withdrawn
 * @param verified      whether the giver was established as entitled to decide (only verified decisions count)
 */
public record GiverDecision(UUID giverPersonId, boolean granted, ConsentScope scope, OffsetDateTime validFrom,
                            LocalDate validUntil, boolean verified) {
}
