package com.mysms.demoseed.seed;

/**
 * One step of the seed pipeline (Strategy): fills the demo school's data of one module, or one coherent part of it,
 * through the API. Each step finds what an earlier run made by its natural keys and creates only what is missing.
 */
public interface ModuleSeeder {

    /** The module whose data this step writes, as the manifest counts it. */
    String module();

    void seed(SeedContext context);
}
