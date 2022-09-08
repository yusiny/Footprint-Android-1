package com.footprint.footprint.domain.usecase

import com.footprint.footprint.data.dto.BaseResponse
import com.footprint.footprint.data.dto.Result
import com.footprint.footprint.domain.repository.CourseRepository

class MarkCourseUseCase(private val repository: CourseRepository) {
    suspend operator fun invoke(courseIdx: Int): Result<BaseResponse> = repository.markCourse(courseIdx)
}