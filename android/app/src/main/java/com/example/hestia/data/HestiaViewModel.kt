package com.example.hestia.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hestia.model.ActivityItem
import com.example.hestia.model.InterestItem
import com.example.hestia.model.Property
import com.example.hestia.model.User
import com.example.hestia.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class HestiaViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("hestia_session", 0)
    private val api get() = ApiProvider.api

    private val _loggedIn = MutableStateFlow(!prefs.getString("token", null).isNullOrBlank())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _user = MutableStateFlow(
        User(
            prefs.getInt("id", 0),
            prefs.getString("name", "Demo Buyer") ?: "Demo Buyer",
            prefs.getString("email", "demo@hestia.com") ?: "demo@hestia.com",
            prefs.getString("phone", "") ?: "",
            prefs.getString("role", "BUYER") ?: "BUYER"
        )
    )
    val user: StateFlow<User> = _user.asStateFlow()

    private val _properties = MutableStateFlow<List<Property>>(emptyList())
    val properties: StateFlow<List<Property>> = _properties.asStateFlow()
    private val _selected = MutableStateFlow<Property?>(null)
    val selected: StateFlow<Property?> = _selected.asStateFlow()
    private val _favourites = MutableStateFlow<Set<Int>>(emptySet())
    val favourites: StateFlow<Set<Int>> = _favourites.asStateFlow()
    private val _activities = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activities: StateFlow<List<ActivityItem>> = _activities.asStateFlow()
    private val _interests = MutableStateFlow<List<InterestItem>>(emptyList())
    val interests: StateFlow<List<InterestItem>> = _interests.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        ApiProvider.init(app)
        if (_loggedIn.value) refreshAll()
    }

    fun clearMessage() { _message.value = null }

    fun login(email: String, password: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        _loading.value = true
        try {
            val r = api.login(LoginRequest(email.trim(), password))
            if (!r.success || r.data == null) {
                _message.value = r.message ?: "Invalid email or password"
                onDone(false)
            } else {
                saveSession(r.data.token, r.data.user)
                _loggedIn.value = true
                refreshAllInternal()
                onDone(true)
            }
        } catch (_: Exception) {
            _message.value = "Server unavailable. Check that HESTIA backend is running."
            onDone(false)
        } finally { _loading.value = false }
    }

    fun register(name: String, email: String, password: String, phone: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        _loading.value = true
        try {
            val r = api.register(RegisterRequest(name.trim(), email.trim(), password, phone.trim()))
            if (r.success) { _message.value = "Account created. Please sign in."; onDone(true) }
            else { _message.value = r.message ?: "Registration failed"; onDone(false) }
        } catch (_: Exception) { _message.value = "Cannot reach HESTIA server."; onDone(false) }
        finally { _loading.value = false }
    }

    private fun saveSession(token: String, u: ApiUser) {
        prefs.edit()
            .putString("token", token)
            .putInt("id", u.id)
            .putString("name", u.name)
            .putString("email", u.email)
            .putString("phone", u.phone ?: "")
            .putString("role", u.role)
            .apply()
        _user.value = User(u.id, u.name, u.email, u.phone, u.role)
    }

    fun logout() {
        prefs.edit().clear().apply()
        _loggedIn.value = false
        _properties.value = emptyList(); _selected.value = null
        _favourites.value = emptySet(); _activities.value = emptyList(); _interests.value = emptyList()
    }

    fun refreshAll() = viewModelScope.launch { refreshAllInternal() }

    private suspend fun refreshAllInternal() {
        loadPropertiesInternal()
        loadFavouritesInternal()
        loadInterestsInternal()
        loadActivityInternal()
    }

    fun refreshProperties(search: String? = null, type: String? = null, minPrice: Double? = null, maxPrice: Double? = null, bedrooms: Int? = null, city: String? = null, includeAll: Boolean = false) = viewModelScope.launch {
        _loading.value = true
        try {
            val r = api.properties(search, type, minPrice, maxPrice, bedrooms, city, if (includeAll) null else "AVAILABLE")
            if (r.success && r.data != null) {
                _properties.value = r.data.map { it.toProperty() }
            } else _message.value = r.message ?: "Could not load properties"
        } catch (_: Exception) { _message.value = "Cannot load properties from server." }
        finally { _loading.value = false }
    }

    private suspend fun loadPropertiesInternal() {
        try {
            val r = api.properties(status = "AVAILABLE")
            if (r.success && r.data != null) _properties.value = r.data.map { it.toProperty() }
        } catch (_: Exception) { }
    }

    fun openProperty(id: Int) = viewModelScope.launch {
        _loading.value = true
        try {
            val r = api.property(id)
            if (r.success && r.data != null) {
                _selected.value = r.data.toProperty()
                runCatching { api.logActivity(ActivityBody("VIEWED", id, "Viewed property details")) }
            } else _message.value = r.message ?: "Property not found"
        } catch (_: Exception) { _message.value = "Unable to open property." }
        finally { _loading.value = false }
    }

    fun clearSelected() { _selected.value = null }

    fun toggleFavourite(property: Property) = viewModelScope.launch {
        val id = property.id
        try {
            if (id in _favourites.value) {
                val r = api.removeFavourite(id)
                if (r.success) { _favourites.value = _favourites.value - id; _message.value = "Removed from saved" }
                else _message.value = r.message ?: "Could not remove favourite"
            } else {
                val r = api.addFavourite(FavouriteBody(id))
                if (r.success) { _favourites.value = _favourites.value + id; _message.value = "Saved to favourites" }
                else _message.value = r.message ?: "Could not save property"
            }
            loadActivityInternal()
        } catch (_: Exception) { _message.value = "Favourite action failed." }
    }

    fun expressInterest(property: Property) = viewModelScope.launch {
        if (_interests.value.any { it.propertyId == property.id }) { _message.value = "Interest already expressed"; return@launch }
        try {
            val r = api.addInterest(InterestBody(property.id))
            if (r.success) { _message.value = "Interest expressed"; loadInterestsInternal(); loadActivityInternal() }
            else _message.value = r.message ?: "Could not express interest"
        } catch (_: Exception) { _message.value = "Interest request failed." }
    }

    fun logEmi(propertyId: Int?) = viewModelScope.launch {
        runCatching { api.logActivity(ActivityBody("EMI_CALCULATED", propertyId, "Calculated home loan EMI")) }
        loadActivityInternal()
    }

    private suspend fun loadFavouritesInternal() {
        try {
            val r = api.favourites()
            if (r.success && r.data != null) _favourites.value = r.data.map { it.resolvedId() }.toSet()
        } catch (_: Exception) { }
    }

    private suspend fun loadInterestsInternal() {
        try {
            val r = api.interests()
            if (r.success && r.data != null) _interests.value = r.data.map { InterestItem(it.id, it.property_id, it.property_title, it.location, it.status, it.created_at) }
        } catch (_: Exception) { }
    }

    private suspend fun loadActivityInternal() {
        try {
            val r = api.activity()
            if (r.success && r.data != null) _activities.value = r.data.map { ActivityItem(it.id.ifZeroUse(it.activity_id), it.activity_type, it.activity_details ?: "", it.property_title, it.property_id, it.created_at) }
        } catch (_: Exception) { }
    }

    fun createProperty(title: String, type: String, location: String, city: String, price: Double, area: Double, bedrooms: Int?, bathrooms: Int?, image: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            val r = api.createProperty(PropertyPayload(title, "Premium HESTIA listing", type, location, city, price = price, area_sqft = area, bedrooms = bedrooms, bathrooms = bathrooms, images = listOf(image).filter { it.isNotBlank() }))
            if (r.success) { _message.value = "Property created"; refreshProperties(includeAll = true); onDone(true) } else { _message.value = r.message ?: "Create failed"; onDone(false) }
        } catch (_: Exception) { _message.value = "Create request failed"; onDone(false) }
    }

    fun deleteProperty(id: Int) = viewModelScope.launch {
        try {
            val r = api.deleteProperty(id)
            if (r.success) { _properties.value = _properties.value.filterNot { it.id == id }; _message.value = "Property deleted" }
            else _message.value = r.message ?: "Delete failed"
        } catch (_: Exception) { _message.value = "Delete request failed" }
    }

    fun updateStatus(id: Int, status: String) = viewModelScope.launch {
        try {
            val r = api.updateStatus(id, StatusPayload(status))
            if (r.success) { _message.value = "Status updated"; refreshProperties(includeAll = true) }
            else _message.value = r.message ?: "Status update failed"
        } catch (_: Exception) { _message.value = "Status update failed" }
    }

    private fun ApiProperty.toProperty(): Property = Property(
        id = resolvedId(), title = title, description = description.orEmpty(), type = property_type,
        location = location, city = city, price = price, area = area_sqft,
        bedrooms = bedrooms ?: 0, bathrooms = bathrooms ?: 0, status = status,
        primaryImage = primary_image,
        images = (images.orEmpty().mapNotNull { it.url } + listOfNotNull(primary_image)).distinct()
    )

    private fun ApiProperty.resolvedId(): Int = if (id != 0) id else property_id
    private fun Int.ifZeroUse(other: Int): Int = if (this != 0) this else other
}

fun formatPrice(value: Double): String {
    return when {
        value >= 10_000_000 -> String.format(Locale.US, "₹%.2f Cr", value / 10_000_000.0)
        value >= 100_000 -> String.format(Locale.US, "₹%.1f L", value / 100_000.0)
        else -> String.format(Locale.US, "₹%,.0f", value)
    }
}
