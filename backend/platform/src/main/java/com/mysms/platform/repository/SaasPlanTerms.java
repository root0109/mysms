package com.mysms.platform.repository;

/** The usage limits of a plan, copied onto a subscription when it is bought (NULL = unlimited). */
public record SaasPlanTerms(Integer maxMembers, Integer maxEvents, Integer maxFolders, Integer maxFiles,
                            Integer maxVideos, Integer maxAudios, Integer maxBulletins, Integer maxGroups) {
}
