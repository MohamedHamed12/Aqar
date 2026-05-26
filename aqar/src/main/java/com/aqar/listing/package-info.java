/**
 * Listing module — owns Listing entities, repositories, services, and controllers.
 *
 * Responsibilities:
 * - Domain: Listing, PriceHistory, ListingImages
 * - Publish events to outbox on create/update/delete
 * - Provide services used by other modules via interfaces only
 *
 * See docs/architecture/ARCHITECTURE_STYLE.md for architecture guidelines.
 */
package com.aqar.listing;
