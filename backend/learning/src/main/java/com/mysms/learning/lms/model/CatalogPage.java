package com.mysms.learning.lms.model;

import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.CatalogHome;
import com.mysms.learning.lms.api.model.LmsCourse;

/**
 * The catalogue home as the service reads it: the home's banners, categories and open cohorts, and the page of listed
 * courses whose navigation links only the controller can build (it knows the request's address).
 */
public record CatalogPage(CatalogHome home, Page<LmsCourse> courses) {
}
