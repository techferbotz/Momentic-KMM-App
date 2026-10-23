package com.ferbotz.momentic.data.model

/**
 * One page of a `?cursor=&limit=` listing (`API.md` § Pagination). The cursor is **opaque**: pass it
 * back verbatim, never parse or construct one. A null [nextCursor] means this was the last page.
 */
data class Page<T>(
    val items: List<T>,
    val nextCursor: String?,
) {
    val hasMore: Boolean get() = nextCursor != null
}
