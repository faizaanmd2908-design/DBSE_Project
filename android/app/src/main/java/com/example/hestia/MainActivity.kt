package com.example.hestia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.hestia.data.HestiaViewModel
import com.example.hestia.data.formatPrice
import com.example.hestia.model.ActivityItem
import com.example.hestia.model.Property
import com.example.hestia.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.pow
import androidx.compose.foundation.border

private enum class Screen { SPLASH, LOGIN, REGISTER, HOME, SAVED, ACTIVITY, PROFILE, DETAIL, EMI, ADMIN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HestiaApp() }
    }
}

@Composable
fun HestiaApp(vm: HestiaViewModel = viewModel()) {
    val loggedIn by vm.loggedIn.collectAsState()
    HestiaTheme {
        var screen by rememberSaveable { mutableStateOf(if (loggedIn) Screen.HOME.name else Screen.SPLASH.name) }
        var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
        val selected by vm.selected.collectAsState()
        val properties by vm.properties.collectAsState()
        val favourites by vm.favourites.collectAsState()
        val activities by vm.activities.collectAsState()
        val message by vm.message.collectAsState()

        LaunchedEffect(loggedIn) {
            if (loggedIn && screen in listOf(Screen.SPLASH.name, Screen.LOGIN.name, Screen.REGISTER.name)) screen = Screen.HOME.name
            if (!loggedIn && screen !in listOf(Screen.SPLASH.name, Screen.LOGIN.name, Screen.REGISTER.name)) screen = Screen.LOGIN.name
        }

        if (message != null) {
            // A simple transient message area; cleared after a short interval.
            LaunchedEffect(message) { delay(2600); vm.clearMessage() }
        }

        BackHandler(enabled = screen != Screen.HOME.name && screen != Screen.LOGIN.name && screen != Screen.SPLASH.name) {
            when (Screen.valueOf(screen)) {
                Screen.DETAIL -> { vm.clearSelected(); screen = Screen.HOME.name }
                Screen.EMI -> screen = Screen.DETAIL.name
                Screen.REGISTER -> screen = Screen.LOGIN.name
                Screen.SAVED, Screen.ACTIVITY, Screen.PROFILE -> screen = Screen.HOME.name
                Screen.ADMIN -> screen = Screen.PROFILE.name
                else -> screen = Screen.HOME.name
            }
        }

        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (Screen.valueOf(screen)) {
                Screen.SPLASH -> SplashScreen { screen = if (loggedIn) Screen.HOME.name else Screen.LOGIN.name }
                Screen.LOGIN -> LoginScreen(vm) { screen = Screen.HOME.name }
                Screen.REGISTER -> RegisterScreen(vm) { screen = Screen.LOGIN.name }
                Screen.HOME -> HomeScreen(vm, properties, favourites, onProperty = { selectedId = it.id; vm.openProperty(it.id); screen = Screen.DETAIL.name }, onNavigate = { screen = it.name })
                Screen.SAVED -> SavedScreen(vm, properties, favourites, onProperty = { selectedId = it.id; vm.openProperty(it.id); screen = Screen.DETAIL.name }, onNavigate = { screen = it.name })
                Screen.ACTIVITY -> ActivityScreen(activities, onBack = { screen = Screen.HOME.name })
                Screen.PROFILE -> ProfileScreen(vm, onBack = { screen = Screen.HOME.name }, onAdmin = { screen = Screen.ADMIN.name }, onLogout = { vm.logout(); screen = Screen.LOGIN.name })
                Screen.DETAIL -> PropertyDetailsScreen(vm, selected ?: selectedId?.let { properties.find { p -> p.id == it } }, favourites, onBack = { vm.clearSelected(); screen = Screen.HOME.name }, onEmi = { screen = Screen.EMI.name })
                Screen.EMI -> EmiScreen(vm, selected ?: selectedId?.let { properties.find { p -> p.id == it } }, onBack = { screen = Screen.DETAIL.name })
                Screen.ADMIN -> AdminScreen(vm, onBack = { screen = Screen.PROFILE.name })
            }
            if (message != null && screen !in listOf(Screen.LOGIN.name, Screen.REGISTER.name)) {
                Text(
                    text = message.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp, vertical = 84.dp)
                        .background(HestiaInk, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) { delay(1200); onDone() }
    Box(Modifier.fillMaxSize().background(HestiaInk), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(HestiaGoldLight), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.HomeWork, null, tint = HestiaInk, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("HESTIA", color = HestiaCream, fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            Spacer(Modifier.height(6.dp))
            Text("Find Where You Belong", color = HestiaCream2, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun LoginScreen(vm: HestiaViewModel, onSuccess: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showRegister by rememberSaveable { mutableStateOf(false) }
    if (showRegister) { RegisterScreen(vm) { showRegister = false }; return }

    Box(Modifier.fillMaxSize().background(HestiaCream)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(320.dp)) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1400&q=90",
                        contentDescription = "Luxury home",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, HestiaInk.copy(alpha = .95f)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(HestiaGoldLight), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.HomeWork, null, tint = HestiaInk, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("HESTIA", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, fontSize = 19.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Find where you belong.", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("Curated homes. Clear decisions.", color = Color.White.copy(.8f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().offset(y = (-22).dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = HestiaCream),
                    border = BorderStroke(1.dp, HestiaBorder)
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
                        Text("Sign in to continue your search.", color = HestiaMuted)
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, leadingIcon = { Icon(Icons.Outlined.Email, null) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(15.dp))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, trailingIcon = { IconButton({ showPassword = !showPassword }) { Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) } }, singleLine = true, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(15.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Forgot password?", Modifier.align(Alignment.End).clickable { vm.clearMessage(); }, color = HestiaTerracotta, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.login(email, password) { ok -> if (ok) onSuccess() } }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) {
                            Text("Sign in", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("New to HESTIA? ", color = HestiaMuted)
                            Text("Create account", fontWeight = FontWeight.Bold, color = HestiaTerracotta, modifier = Modifier.clickable { showRegister = true })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterScreen(vm: HestiaViewModel, onDone: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(HestiaCream).padding(22.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))
        Text("Create your account", style = MaterialTheme.typography.headlineLarge)
        Text("A better way to discover your next home.", color = HestiaMuted)
        Spacer(Modifier.height(22.dp))
        listOf("Full name" to name, "Email" to email, "Phone" to phone).forEach { } // keeps order explicit below
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("Phone") }, singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, shape = RoundedCornerShape(14.dp), visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, singleLine = true, shape = RoundedCornerShape(14.dp), visualTransformation = PasswordVisualTransformation())
        Spacer(Modifier.height(20.dp))
        Button(onClick = { if (password == confirm && name.isNotBlank() && email.isNotBlank()) vm.register(name, email, password, phone) { ok -> if (ok) onDone() } else vm.clearMessage() }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(15.dp)) { Text("Create account", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onDone, Modifier.align(Alignment.CenterHorizontally)) { Text("Back to sign in") }
    }
}

@Composable
private fun HomeScreen(vm: HestiaViewModel, properties: List<Property>, favourites: Set<Int>, onProperty: (Property) -> Unit, onNavigate: (Screen) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("All") }
    val local = properties.filter { p ->
        val q = query.trim()
        (q.isBlank() || p.title.contains(q, true) || p.location.contains(q, true) || p.city.contains(q, true)) &&
            (selectedType == "All" || p.type.equals(selectedType, true))
    }
    Scaffold(bottomBar = { BottomBar(Screen.HOME, onNavigate) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("HESTIA", color = HestiaTerracotta, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            Text("Good evening, ${vm.user.value.name.substringBefore(' ')}", style = MaterialTheme.typography.titleLarge)
                        }
                        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(HestiaCream2), contentAlignment = Alignment.Center) {
                            Text(vm.user.value.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Find a place you'll love.", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(5.dp))
                    Text("Discover homes across Hyderabad, made easy.", color = HestiaMuted)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(query, { query = it }, Modifier.weight(1f), placeholder = { Text("Search homes, areas or cities") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = RoundedCornerShape(17.dp))
                        Spacer(Modifier.width(10.dp))
                        IconButton(onClick = { }) { Icon(Icons.Filled.Tune, contentDescription = "Filters") }
                    }
                }
            }
            item {
                Spacer(Modifier.height(18.dp))
                Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(210.dp).clip(RoundedCornerShape(28.dp))) {
                    AsyncImage("https://images.unsplash.com/photo-1600607687920-4e2a09cf159d?w=1400&q=90", "HESTIA featured homes", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, HestiaInk.copy(.82f)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(19.dp)) {
                        Text("HESTIA EDIT", color = HestiaGoldLight, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("Spaces that feel like you.", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Text("Thoughtfully selected homes in Hyderabad.", color = Color.White.copy(.78f))
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Apartment", "Villa", "House", "Plot")) { type ->
                        FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(type) }, shape = RoundedCornerShape(50))
                    }
                }
            }
            item { SectionTitle("Featured homes", "View ${local.size} matches", Modifier.padding(horizontal = 20.dp)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(local.take(6), key = { it.id }) { p -> FeaturedCard(p, p.id in favourites, { onProperty(p) }, { vm.toggleFavourite(p) }) }
                }
            }
            item { SectionTitle("Explore properties", "", Modifier.padding(horizontal = 20.dp)) }
            items(local, key = { it.id }) { p -> PropertyRow(p, p.id in favourites, { onProperty(p) }, { vm.toggleFavourite(p) }, Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) }
            if (local.isEmpty()) item { EmptyBlock("No homes found", "Try another area or property type.") }
        }
    }
}

@Composable
private fun FeaturedCard(p: Property, fav: Boolean, onClick: () -> Unit, onFav: () -> Unit) {
    Card(Modifier.width(290.dp).clickable { onClick() }, RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(HestiaSurface)) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            PropertyImage(p, Modifier.fillMaxSize())
            Badge(p.type, Modifier.align(Alignment.TopStart).padding(12.dp))
            IconButton(onClick = onFav, Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.White.copy(.9f), CircleShape)) {
                Icon(if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (fav) HestiaTerracotta else HestiaInk)
            }
        }
        Column(Modifier.padding(15.dp)) {
            Text(p.title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
            Text("${p.location}, ${p.city}", color = HestiaMuted, maxLines = 1)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatPrice(p.price), fontWeight = FontWeight.Bold)
                Text("${p.bedrooms} bd · ${p.bathrooms} ba", color = HestiaMuted)
            }
        }
    }
}

@Composable
private fun PropertyRow(p: Property, fav: Boolean, onClick: () -> Unit, onFav: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable { onClick() }, RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(HestiaSurface), border = BorderStroke(1.dp, HestiaBorder)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(120.dp).clip(RoundedCornerShape(17.dp))) {
                PropertyImage(p, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.title, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                    IconButton(onClick = onFav, Modifier.size(34.dp)) { Icon(if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (fav) HestiaTerracotta else HestiaInkSoft, modifier = Modifier.size(20.dp)) }
                }
                Text("${p.location}, ${p.city}", color = HestiaMuted, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Text(formatPrice(p.price), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(3.dp))
                Text("${p.area.toInt()} sqft  ·  ${p.bedrooms} bed  ·  ${p.bathrooms} bath", color = HestiaMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PropertyImage(p: Property, modifier: Modifier) {
    val url = p.primaryImage ?: p.images.firstOrNull()
    Box(modifier.background(HestiaCream2), contentAlignment = Alignment.Center) {
        if (!url.isNullOrBlank()) AsyncImage(url, p.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (url.isNullOrBlank()) Icon(Icons.Filled.HomeWork, null, tint = HestiaGold, modifier = Modifier.size(42.dp))
    }
}

@Composable
private fun PropertyDetailsScreen(vm: HestiaViewModel, property: Property?, favourites: Set<Int>, onBack: () -> Unit, onEmi: () -> Unit) {
    if (property == null) { EmptyBlock("Property unavailable", "Please return to the home screen."); return }
    val fav = property.id in favourites
    val interested = vm.interests.collectAsState().value.any { it.propertyId == property.id }
    val gallery = (property.images.ifEmpty { listOfNotNull(property.primaryImage) }).distinct()
    var imageIndex by rememberSaveable(property.id) { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0) { gallery.size.coerceAtLeast(1) }
    val galleryScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) { imageIndex = pagerState.currentPage }

    Column(Modifier.fillMaxSize().background(HestiaCream)) {
        Box(Modifier.fillMaxWidth().height(340.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val url = gallery.getOrNull(page)
                Box(Modifier.fillMaxSize().background(HestiaCream2)) {
                    if (url != null) AsyncImage(url, property.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Filled.HomeWork, null, tint = HestiaGold, modifier = Modifier.align(Alignment.Center).size(55.dp))
                }
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(HestiaInk.copy(.25f), Color.Transparent, HestiaInk.copy(.65f)))))
            IconButton(onClick = onBack, Modifier.padding(12.dp).background(Color.White.copy(.92f), CircleShape)) { Icon(Icons.Filled.ArrowBack, null) }
            IconButton(onClick = { vm.toggleFavourite(property) }, Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color.White.copy(.92f), CircleShape)) { Icon(if (fav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = if (fav) HestiaTerracotta else HestiaInk) }
            if (gallery.size > 1) {
                Text("${imageIndex + 1} / ${gallery.size}", Modifier.align(Alignment.BottomEnd).padding(16.dp).background(HestiaInk.copy(.65f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (gallery.size > 1) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                itemsIndexed(gallery) { index, g ->
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(HestiaCream2)
                            .clickable { galleryScope.launch { pagerState.animateScrollToPage(index) } }
                            .then(if (imageIndex == index) Modifier else Modifier)
                    ) {
                        AsyncImage(g, property.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (imageIndex == index) {
                            Box(Modifier.fillMaxSize().background(HestiaInk.copy(alpha = .18f)))
                            Box(Modifier.fillMaxSize().padding(3.dp).border(2.dp, HestiaGoldLight, RoundedCornerShape(10.dp)))
                        }
                    }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(property.title, style = MaterialTheme.typography.headlineMedium)
                        Text("${property.location}, ${property.city}", color = HestiaMuted)
                    }
                    Surface(color = HestiaGoldLight, shape = RoundedCornerShape(12.dp)) { Text(property.type, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium) }
                }
                Spacer(Modifier.height(12.dp))
                Text(formatPrice(property.price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailStat("Area", "${property.area.toInt()} sqft", Modifier.weight(1f))
                    DetailStat("Beds", property.bedrooms.toString(), Modifier.weight(1f))
                    DetailStat("Baths", property.bathrooms.toString(), Modifier.weight(1f))
                }
            }
            item { SectionTitle("About this property", "", Modifier.padding(top = 22.dp)) }
            item { Text(property.description.ifBlank { "A thoughtfully planned home with comfortable spaces, excellent connectivity and everyday amenities." }, color = HestiaMuted, lineHeight = 22.sp) }
            item { SectionTitle("Features & amenities", "", Modifier.padding(top = 22.dp)) }
            item {
                val amenities = when (property.type.uppercase()) {
                    "VILLA", "HOUSE" -> listOf("Parking", "24/7 Security", "Power Backup", "Garden", "Balcony", "Road Access")
                    "PLOT" -> listOf("Gated Layout", "Wide Roads", "24/7 Security", "Park Nearby")
                    else -> listOf("Parking", "24/7 Security", "Power Backup", "Gym", "Swimming Pool", "Garden")
                }
                FlowWrap(amenities)
            }
            item { Spacer(Modifier.height(24.dp))
                Button(onClick = { if (!interested) vm.expressInterest(property) }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) {
                    Icon(if (interested) Icons.Filled.CheckCircle else Icons.Filled.ThumbUp, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (interested) "Interest Expressed" else "Express Interest")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onEmi, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.Calculate, null); Spacer(Modifier.width(8.dp)); Text("Calculate EMI") }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = HestiaSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, HestiaBorder)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = HestiaMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FlowWrap(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    Surface(Modifier.weight(1f), color = HestiaSurface, shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, HestiaBorder)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CheckCircleOutline, null, tint = HestiaGold); Spacer(Modifier.width(8.dp)); Text(item, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedScreen(vm: HestiaViewModel, properties: List<Property>, favourites: Set<Int>, onProperty: (Property) -> Unit, onNavigate: (Screen) -> Unit) {
    val saved = properties.filter { it.id in favourites }
    Scaffold(bottomBar = { BottomBar(Screen.SAVED, onNavigate) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp)) {
            item { Text("Saved homes", style = MaterialTheme.typography.headlineLarge); Text("${saved.size} properties saved", color = HestiaMuted); Spacer(Modifier.height(18.dp)) }
            items(saved, key = { it.id }) { p -> PropertyRow(p, true, { onProperty(p) }, { vm.toggleFavourite(p) }, Modifier.padding(bottom = 12.dp)) }
            if (saved.isEmpty()) item { EmptyBlock("Nothing saved yet", "Tap the heart on a property to keep it here.") }
        }
    }
}

@Composable
private fun ActivityScreen(items: List<ActivityItem>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(HestiaCream)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }; Column { Text("Activity", style = MaterialTheme.typography.headlineSmall); Text("Your recent HESTIA journey", color = HestiaMuted) } }
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            if (items.isEmpty()) item { EmptyBlock("No activity yet", "Your views, saves and interests will appear here.") }
            items(items, key = { it.id }) { a -> ActivityCard(a) }
        }
    }
}

@Composable
private fun ActivityCard(a: ActivityItem) {
    val icon = when (a.type) { "SAVED" -> Icons.Filled.Favorite; "UNSAVED" -> Icons.Filled.FavoriteBorder; "INTERESTED" -> Icons.Filled.ThumbUp; "EMI_CALCULATED" -> Icons.Filled.Calculate; else -> Icons.Filled.Visibility }
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp), RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(HestiaSurface)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(HestiaGoldLight), contentAlignment = Alignment.Center) { Icon(icon, null, tint = HestiaInk, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(a.type.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold); Text(a.propertyTitle ?: a.details, color = HestiaMuted); Text(a.createdAt ?: "", color = HestiaMuted, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun EmiScreen(vm: HestiaViewModel, property: Property?, onBack: () -> Unit) {
    var loan by rememberSaveable { mutableStateOf(property?.price?.toInt()?.toString() ?: "5000000") }
    var rate by rememberSaveable { mutableStateOf("8.5") }
    var years by rememberSaveable { mutableStateOf("20") }
    var emi by remember { mutableStateOf<Double?>(null) }
    Column(Modifier.fillMaxSize().background(HestiaCream).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }; Text("EMI calculator", style = MaterialTheme.typography.headlineSmall) }
        Column(Modifier.padding(horizontal = 20.dp)) {
            property?.let { Text("For ${it.title}", color = HestiaMuted); Spacer(Modifier.height(8.dp)) }
            OutlinedTextField(loan, { loan = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Loan amount") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(rate, { rate = it.filter { c -> c.isDigit() || c == '.' } }, Modifier.fillMaxWidth(), label = { Text("Interest rate (% p.a.)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(years, { years = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Tenure (years)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(18.dp))
            Button(onClick = {
                val p = loan.toDoubleOrNull(); val annual = rate.toDoubleOrNull(); val y = years.toIntOrNull();
                if (p != null && annual != null && y != null && p > 0 && annual >= 0 && y > 0) {
                    val r = annual / 12 / 100; val n = y * 12
                    emi = if (r == 0.0) p / n else p * r * (1 + r).pow(n.toDouble()) / ((1 + r).pow(n.toDouble()) - 1)
                    vm.logEmi(property?.id)
                }
            }, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.Calculate, null); Spacer(Modifier.width(8.dp)); Text("Calculate EMI") }
            emi?.let { value ->
                Spacer(Modifier.height(22.dp))
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(HestiaInk)) {
                    Column(Modifier.padding(22.dp)) { Text("Estimated monthly EMI", color = HestiaCream2); Text(formatPrice(value), color = HestiaCream, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); Divider(color = HestiaCream.copy(.2f)); Spacer(Modifier.height(10.dp)); val principal = loan.toDoubleOrNull() ?: 0.0; val months = (years.toIntOrNull() ?: 1) * 12; val total = value * months; Text("Total payment  ${formatPrice(total)}", color = HestiaCream2); Text("Total interest  ${formatPrice((total - principal).coerceAtLeast(0.0))}", color = HestiaCream2) }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ProfileScreen(vm: HestiaViewModel, onBack: () -> Unit, onAdmin: () -> Unit, onLogout: () -> Unit) {
    val user by vm.user.collectAsState()
    Column(Modifier.fillMaxSize().background(HestiaCream).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }; Text("Profile", style = MaterialTheme.typography.headlineSmall) }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(HestiaGoldLight), contentAlignment = Alignment.Center) { Text(user.name.take(1).uppercase(), fontSize = 30.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(15.dp)); Column { Text(user.name, style = MaterialTheme.typography.headlineSmall); Text(user.email, color = HestiaMuted); Text(user.role, color = HestiaTerracotta, fontWeight = FontWeight.SemiBold) }
            }
            SectionTitle("Personal information", "", Modifier.padding(top = 26.dp))
            ProfileLine(Icons.Outlined.Person, "Full name", user.name)
            ProfileLine(Icons.Outlined.Email, "Email", user.email)
            ProfileLine(Icons.Outlined.Phone, "Phone", user.phone?.ifBlank { "Not added" } ?: "Not added")
            SectionTitle("Account", "", Modifier.padding(top = 18.dp))
            ProfileLine(Icons.Outlined.FavoriteBorder, "Saved properties", "Manage your saved homes")
            ProfileLine(Icons.Outlined.History, "Activity history", "Views, saves, interests and EMI")
            if (user.role == "ADMIN") {
                Spacer(Modifier.height(10.dp)); Button(onClick = onAdmin, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.AdminPanelSettings, null); Spacer(Modifier.width(8.dp)); Text("Open admin console") }
            }
            Spacer(Modifier.height(14.dp)); OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out") }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ProfileLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = HestiaTerracotta, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(14.dp)); Column { Text(label, color = HestiaMuted, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.bodyLarge) } }
}

@Composable
private fun AdminScreen(vm: HestiaViewModel, onBack: () -> Unit) {
    val props by vm.properties.collectAsState()
    LaunchedEffect(Unit) { vm.refreshProperties(includeAll = true) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(HestiaCream)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }; Column { Text("Admin console", style = MaterialTheme.typography.headlineSmall); Text("Property inventory", color = HestiaMuted) } }
            FloatingActionButton(onClick = { showAdd = true }, containerColor = HestiaInk, contentColor = HestiaCream, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Add, null) }
        }
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AdminStat("Listings", props.size.toString(), Modifier.weight(1f)); AdminStat("Available", props.count { it.status == "AVAILABLE" }.toString(), Modifier.weight(1f)); AdminStat("Reserved", props.count { it.status == "RESERVED" }.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(13.dp))
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            items(props, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth().padding(bottom = 9.dp), RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(HestiaSurface)) {
                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(65.dp).clip(RoundedCornerShape(12.dp))) { PropertyImage(p, Modifier.fillMaxSize()) }
                        Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(p.title, fontWeight = FontWeight.SemiBold, maxLines = 1); Text(formatPrice(p.price), color = HestiaMuted); Text(p.status, color = if (p.status == "AVAILABLE") HestiaTerracotta else HestiaMuted, style = MaterialTheme.typography.labelSmall) }
                        IconButton(onClick = { vm.deleteProperty(p.id) }) { Icon(Icons.Filled.DeleteOutline, null, tint = HestiaRed) }
                    }
                }
            }
        }
    }
    if (showAdd) AddPropertyDialog(vm) { showAdd = false }
}

@Composable
private fun AdminStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(HestiaSurface), border = BorderStroke(1.dp, HestiaBorder)) { Column(Modifier.padding(13.dp)) { Text(label, color = HestiaMuted, style = MaterialTheme.typography.labelSmall); Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun AddPropertyDialog(vm: HestiaViewModel, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var area by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onClose, confirmButton = { Button(onClick = { if (title.isNotBlank() && location.isNotBlank() && price.isNotBlank() && area.isNotBlank()) vm.createProperty(title, "APARTMENT", location, "Hyderabad", price.toDoubleOrNull() ?: 0.0, area.toDoubleOrNull() ?: 0.0, 2, 2, "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=900&q=85") { if (it) onClose() } }) { Text("Create") } }, dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }, title = { Text("Add property") }, text = { Column { OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true); Spacer(Modifier.height(8.dp)); OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true); Spacer(Modifier.height(8.dp)); OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true); Spacer(Modifier.height(8.dp)); OutlinedTextField(area, { area = it.filter(Char::isDigit) }, label = { Text("Area sqft") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) } })
}

@Composable
private fun BottomBar(current: Screen, onNavigate: (Screen) -> Unit) {
    NavigationBar(containerColor = HestiaSurface) {
        val items = listOf(Screen.HOME to (Icons.Filled.Home to "Home"), Screen.SAVED to (Icons.Filled.Favorite to "Saved"), Screen.ACTIVITY to (Icons.Filled.History to "Activity"), Screen.PROFILE to (Icons.Filled.Person to "Profile"))
        items.forEach { (screen, pair) -> NavigationBarItem(selected = current == screen, onClick = { onNavigate(screen) }, icon = { Icon(pair.first, null) }, label = { Text(pair.second) }) }
    }
}

@Composable private fun SectionTitle(title: String, action: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Text(title, style = MaterialTheme.typography.headlineSmall); if (action.isNotBlank()) Text(action, color = HestiaTerracotta, style = MaterialTheme.typography.labelLarge) }
}

@Composable private fun Badge(text: String, modifier: Modifier = Modifier) { Surface(modifier, color = Color.White.copy(.92f), shape = RoundedCornerShape(50)) { Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) } }

@Composable private fun EmptyBlock(title: String, subtitle: String) { Box(Modifier.fillMaxWidth().padding(vertical = 55.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(62.dp).clip(CircleShape).background(HestiaGoldLight), contentAlignment = Alignment.Center) { Icon(Icons.Filled.HomeWork, null, tint = HestiaInk) }; Spacer(Modifier.height(14.dp)); Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, color = HestiaMuted) } } }
