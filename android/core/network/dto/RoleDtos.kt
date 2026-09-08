package com.nivya.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * Role DTO models matching the Spring Boot backend API.
 */
data class SelectRoleRequestDto(
    @SerializedName("role") val role: String
)

data class RoleInfoResponseDto(
    @SerializedName("role") val role: String,
    @SerializedName("permittedModules") val permittedModules: List<String> = emptyList(),
    @SerializedName("prohibitedModules") val prohibitedModules: List<String> = emptyList()
)
