package com.example.hestia.model

data class Property(
    val id: Int,
    val title: String,
    val description: String,
    val type: String,
    val location: String,
    val city: String,
    val price: Double,
    val area: Double,
    val bedrooms: Int,
    val bathrooms: Int,
    val status: String,
    val primaryImage: String?,
    val images: List<String>
)

data class ActivityItem(
    val id: Int,
    val type: String,
    val details: String,
    val propertyTitle: String?,
    val propertyId: Int?,
    val createdAt: String?
)

data class InterestItem(
    val id: Int,
    val propertyId: Int,
    val propertyTitle: String,
    val location: String,
    val status: String,
    val createdAt: String?
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val role: String
)
