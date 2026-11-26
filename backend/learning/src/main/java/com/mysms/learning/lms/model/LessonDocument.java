package com.mysms.learning.lms.model;

/**
 * A lesson's editor document (lms.lesson.content / teacher_content, jsonb): the block document the lesson editor
 * writes, kept verbatim as JSON text — the contract gives it no schema of its own (an object of the editor's blocks),
 * so the repository casts it to jsonb on the way in and the service turns it into the contract's JSON value.
 */
public record LessonDocument(String json) {
}
