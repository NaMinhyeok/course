package io.github.naminhyeok.course.core.support

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

data class OffsetLimit(
    val offset: Int,
    val limit: Int,
) {
    fun toPageable(sort: Sort = Sort.unsorted()): Pageable = PageRequest.of(offset / limit, limit, sort)
}
