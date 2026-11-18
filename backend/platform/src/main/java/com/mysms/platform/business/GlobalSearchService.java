package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.platform.api.model.SearchHit;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.domain.FileObjectEntity;
import com.mysms.platform.domain.TagEntity;
import com.mysms.platform.repository.FileObjectRepository;
import com.mysms.platform.repository.TagRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * platform.GlobalSearch over what the platform owns: files and tags, each as the caller may see it (a file: their own
 * upload, or every file with a tenant-wide {@code platform.file_object.read}; tags with {@code platform.tag.read}).
 * Students, staff, guardians, courses and library media belong to other modules, which the platform may not read and
 * whose events carry only ids, so asking for those types is 400 rather than a silently empty answer. Hits are ordered
 * by title; pages are numbered (offset at most 10,000 hits).
 */
@Service
public class GlobalSearchService extends AppTransactionManager {

    static final String FILES = FileObjectService.AGGREGATE;
    static final String TAGS = TagService.AGGREGATE;
    static final Set<String> SEARCHABLE = Set.of(FILES, TAGS);
    private static final int MIN_QUERY = 2;
    private static final int MAX_QUERY = 100;

    private final FileObjectRepository files;
    private final TagRepository tags;

    public GlobalSearchService(FileObjectRepository files, TagRepository tags,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions) {
        super(database, transactions);
        this.files = files;
        this.tags = tags;
    }

    public Page<SearchHit> search(String query, List<String> entityTypes, Integer pageNumber, Integer pageSize) {
        String text = query.trim();
        if (text.length() < MIN_QUERY || text.length() > MAX_QUERY) {
            throw new InvalidArgumentException("query: " + MIN_QUERY + " to " + MAX_QUERY + " characters.");
        }
        Set<String> types = types(entityTypes);
        int page = pageNumber == null ? 1 : pageNumber;
        int size = pageSize == null ? PageRequest.DEFAULT_SIZE : pageSize;
        long offset = (long) (page - 1) * size;
        if (offset > PageRequest.MAX_OFFSET) {
            throw new InvalidArgumentException("page: at most " + PageRequest.MAX_OFFSET + " hits can be skipped.");
        }
        int limit = (int) offset + size + 1;
        AuthContext caller = AuthContext.require();
        String pattern = "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        List<SearchHit> hits = doInReadTransaction(() -> {
            List<SearchHit> found = new ArrayList<>();
            if (types.contains(FILES)) {
                files.search(pattern, FileObjectRepository.visibleTo(caller.userId(),
                        caller.hasTenantWidePermission(FileObjectService.PERMISSION_READ)), limit)
                        .forEach(file -> found.add(hit(file)));
            }
            if (types.contains(TAGS) && caller.hasPermission(TagService.PERMISSION_READ)) {
                tags.search(pattern, limit).forEach(tag -> found.add(hit(tag)));
            }
            return found;
        });
        hits.sort(Comparator.comparing((SearchHit h) -> h.getTitle().toLowerCase(Locale.ROOT))
                .thenComparing(SearchHit::getEntityId));
        boolean more = hits.size() > offset + size;
        List<SearchHit> slice = hits.subList((int) Math.min(offset, hits.size()),
                (int) Math.min(offset + size, hits.size()));
        return new Page<>(slice, page, size, more ? "p" + (page + 1) : "");
    }

    /** The requested types, all of the searchable ones when none is named; an unsearchable type is 400. */
    private static Set<String> types(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return SEARCHABLE;
        }
        Set<String> types = new LinkedHashSet<>(requested);
        if (!SEARCHABLE.containsAll(types)) {
            throw new InvalidArgumentException("entityTypes: the platform searches " + SEARCHABLE.stream().sorted()
                    .toList() + " only.");
        }
        return types;
    }

    private static SearchHit hit(FileObjectEntity file) {
        return new SearchHit(FILES, file.getId(), file.getFileName()).subtitle(file.getFileKind())
                .urlPath("/platform/files/" + file.getId());
    }

    private static SearchHit hit(TagEntity tag) {
        return new SearchHit(TAGS, tag.getId(), tag.getName()).subtitle(tag.getTagType())
                .urlPath("/platform/tags/" + tag.getId());
    }
}
