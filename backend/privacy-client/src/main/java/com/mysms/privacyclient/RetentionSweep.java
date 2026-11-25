package com.mysms.privacyclient;

import java.util.List;

/**
 * How a module sweeps one retention policy (design §10), declared with SQL constants of its repository layer:
 * {@code subjects} lists, in ascending order, the data principals with rows due ({@code :cutoff}), after
 * {@code :after}, at most {@code :limit}; each subject's due rows are then erased by {@code steps} (whose selects
 * read {@code :subjectId} and {@code :cutoff}), the rows a hold covers kept.
 *
 * @param subjects the next batch of subjects with rows due
 * @param steps    the tables swept for one subject
 */
public record RetentionSweep(String subjects, List<ErasureStep> steps) {

    public RetentionSweep {
        steps = List.copyOf(steps);
    }
}
