package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Statements of forum moderation and per-user read state: guarded post updates, recomputing which replies hide with a
 * hidden ancestor, and upserts of a user's read and flag marks.
 */
@Repository
public class ForumModerationRepository {

    private static final String SET_HIDDEN = """
            UPDATE lms.forum_post SET is_hidden = :hidden, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String CONFIRM_ABUSE = """
            UPDATE lms.forum_post SET is_abusive = true, is_hidden = true, abuse_confirmed_by = :actor,
                   abuse_confirmed_at = :now, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SET_SUBSTANTIVE = """
            UPDATE lms.forum_post SET substantive_flag = :flag, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    // A reply is hidden by its parent when any post above it in the thread is hidden: the thread is read and walked in
    // Java (no recursive compound statement), then the changed rows are written.
    private static final String THREAD = """
            SELECT p.id, p.parent_post_id, p.is_hidden, p.is_hidden_by_parent
              FROM lms.forum_post p
             WHERE p.tenant_id = :tenantId AND p.forum_topic_id = :topicId AND p.deleted_at IS NULL
            """;
    private static final String SET_HIDDEN_BY_PARENT = """
            UPDATE lms.forum_post SET is_hidden_by_parent = :covered, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = ANY(:ids) AND deleted_at IS NULL
            """;
    private static final String MARK_READ = """
            INSERT INTO lms.forum_read_state (id, tenant_id, user_id, forum_topic_id, forum_post_id, read_at,
                                              created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :topicId, :postId, :now, :now, :userId, :now, :userId, 1)
            ON CONFLICT (user_id, forum_topic_id, COALESCE(forum_post_id, '00000000-0000-0000-0000-000000000000'::uuid))
               WHERE deleted_at IS NULL
            DO UPDATE SET read_at = EXCLUDED.read_at, updated_at = EXCLUDED.updated_at,
                          row_version = lms.forum_read_state.row_version + 1
            """;
    private static final String FLAG = """
            INSERT INTO lms.forum_read_state (id, tenant_id, user_id, forum_topic_id, forum_post_id, read_at,
                                              is_flagged, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :userId, :topicId, :postId, :now, :flagged, :now, :userId, :now, :userId, 1)
            ON CONFLICT (user_id, forum_topic_id, COALESCE(forum_post_id, '00000000-0000-0000-0000-000000000000'::uuid))
               WHERE deleted_at IS NULL
            DO UPDATE SET is_flagged = EXCLUDED.is_flagged, updated_at = EXCLUDED.updated_at,
                          row_version = lms.forum_read_state.row_version + 1
            """;

    private final JdbcClient jdbc;

    public ForumModerationRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public int setHidden(UUID id, int expected, boolean hidden, OffsetDateTime now, UUID actor) {
        return guarded(SET_HIDDEN, id, expected, now, actor).param("hidden", hidden).update();
    }

    public int confirmAbuse(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return guarded(CONFIRM_ABUSE, id, expected, now, actor).update();
    }

    public int setSubstantive(UUID id, int expected, short flag, OffsetDateTime now, UUID actor) {
        return guarded(SET_SUBSTANTIVE, id, expected, now, actor).param("flag", flag).update();
    }

    /** Brings every reply's is_hidden_by_parent of the topic in line with its ancestors. */
    public int recomputeHiddenByParent(UUID topicId, OffsetDateTime now, UUID actor) {
        Map<UUID, ThreadPost> thread = new HashMap<>();
        jdbc.sql(THREAD).param("topicId", topicId).query((rs, n) -> new ThreadPost(rs.getObject("id", UUID.class),
                rs.getObject("parent_post_id", UUID.class), rs.getBoolean("is_hidden"),
                rs.getBoolean("is_hidden_by_parent"))).list().forEach(post -> thread.put(post.id(), post));
        Map<UUID, Boolean> covered = new HashMap<>();
        List<UUID> cover = new ArrayList<>();
        List<UUID> uncover = new ArrayList<>();
        for (ThreadPost post : thread.values()) {
            Boolean due = covered(post.id(), thread, covered);
            if (due != null && due != post.hiddenByParent()) {
                (due ? cover : uncover).add(post.id());
            }
        }
        return setHiddenByParent(cover, true, now, actor) + setHiddenByParent(uncover, false, now, actor);
    }

    /** A live post of a topic as the thread walk reads it. */
    private record ThreadPost(UUID id, UUID parentId, boolean hidden, boolean hiddenByParent) {
    }

    /**
     * Whether a hidden post sits above the post, walking up to the thread's root; null for a post whose chain does not
     * reach a live root (a removed ancestor), which keeps its flag. Results are remembered in {@code known}.
     */
    private static Boolean covered(UUID id, Map<UUID, ThreadPost> thread, Map<UUID, Boolean> known) {
        List<ThreadPost> chain = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        ThreadPost post = thread.get(id);
        Boolean above = null;
        while (post != null && seen.add(post.id())) {
            if (known.containsKey(post.id())) {
                above = known.get(post.id());
                break;
            }
            chain.add(post);
            if (post.parentId() == null) {
                above = false;
                chain.remove(chain.size() - 1);
                known.put(post.id(), false);
                break;
            }
            post = thread.get(post.parentId());
        }
        for (int i = chain.size() - 1; i >= 0; i--) {
            ThreadPost child = chain.get(i);
            ThreadPost parent = thread.get(child.parentId());
            above = above == null ? null : above || parent.hidden();
            known.put(child.id(), above);
        }
        return known.get(id);
    }

    private int setHiddenByParent(List<UUID> ids, boolean covered, OffsetDateTime now, UUID actor) {
        if (ids.isEmpty()) {
            return 0;
        }
        return jdbc.sql(SET_HIDDEN_BY_PARENT).param("ids", ids.toArray(UUID[]::new)).param("covered", covered)
                .param("now", now).param("actor", actor).update();
    }


    /** Records that the user read the topic (post null) or one post of it. */
    public void markRead(UUID id, UUID userId, UUID topicId, UUID postId, OffsetDateTime now) {
        jdbc.sql(MARK_READ).param("id", id).param("userId", userId).param("topicId", topicId).param("postId", postId)
                .param("now", now).update();
    }

    /** Sets the user's own flag on a post. */
    public void flag(UUID id, UUID userId, UUID topicId, UUID postId, boolean flagged, OffsetDateTime now) {
        jdbc.sql(FLAG).param("id", id).param("userId", userId).param("topicId", topicId).param("postId", postId)
                .param("flagged", flagged).param("now", now).update();
    }

    private JdbcClient.StatementSpec guarded(String sql, UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(sql).param("id", id).param("expected", expected).param("now", now).param("actor", actor);
    }
}
