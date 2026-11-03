package com.bhumivera.scanner.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import coil3.compose.AsyncImage
import com.bhumivera.scanner.data.AddCartRequest
import com.bhumivera.scanner.data.BhumiveraService
import com.bhumivera.scanner.data.CartItem
import com.bhumivera.scanner.data.CustomerUser
import com.bhumivera.scanner.data.GoogleLoginRequest
import com.bhumivera.scanner.data.LoginRequest
import com.bhumivera.scanner.data.Product
import com.bhumivera.scanner.data.SessionStore
import com.bhumivera.scanner.data.TwoFactorRequest
import com.bhumivera.scanner.data.UpdateCartRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val IMAGE_BASE_URL =
    "https://pub-70fdb5d94df347c4bed417c28b066c02.r2.dev/bhumivera"

@Composable
fun BhumiveraApp(service: BhumiveraService, sessionStore: SessionStore) {
    val navController = rememberNavController()
    var sessionToken by remember { mutableStateOf(sessionStore.token()) }
    var customer by remember { mutableStateOf<CustomerUser?>(null) }
    // Default to the high-speed Standalone Scanner & E-Bill POS Terminal
    val startDestination = "scanner"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("scanner") {
            ScannerScreen(
                service = service,
                sessionStore = sessionStore,
                onBackToStore = { navController.navigate("shop") }
            )
        }
        composable("login") {
            LoginScreen(
                service = service,
                onSignedIn = { response ->
                    val token = response.token
                    check(!token.isNullOrBlank()) { "Sign-in did not return a valid session." }
                    val role = response.user?.role
                    check(role.isNullOrBlank() || role !in setOf("admin", "superadmin", "warehouse_admin")) {
                        "This app is for customer accounts. Please use the admin sign-in website."
                    }
                    sessionStore.saveToken(token)
                    sessionToken = token
                    customer = response.user
                    navController.navigate("shop") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("shop") {
            NativeShell(
                currentRoute = "shop",
                title = "Bhumivera",
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
            ) { contentPadding ->
                ShopScreen(
                    service = service,
                    contentPadding = contentPadding,
                    onProductSelected = { product ->
                        val slug = product.slug?.takeIf(String::isNotBlank) ?: product.id.toString()
                        val encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8.toString())
                        navController.navigate("product/$encodedSlug")
                    },
                )
            }
        }
        composable(
            route = "product/{slug}",
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { entry ->
            val slug = entry.arguments?.getString("slug").orEmpty()
            NativeShell(
                currentRoute = "shop",
                title = "Product",
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            ) { contentPadding ->
                ProductDetailScreen(
                    service = service,
                    slug = slug,
                    contentPadding = contentPadding,
                    onAddedToCart = { navController.navigate("cart") },
                )
            }
        }
        composable("cart") {
            NativeShell(
                currentRoute = "cart",
                title = "Your bag",
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
            ) { contentPadding ->
                CartScreen(service, contentPadding)
            }
        }
        composable("account") {
            NativeShell(
                currentRoute = "account",
                title = "Account",
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
            ) { contentPadding ->
                AccountScreen(
                    service = service,
                    customer = customer,
                    sessionStore = sessionStore,
                    contentPadding = contentPadding,
                    onSignedOut = {
                        sessionToken = null
                        customer = null
                        navController.navigate("login") {
                            popUpTo("account") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeShell(
    currentRoute: String,
    title: String,
    onNavigate: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                listOf("scanner" to "Scanner POS", "shop" to "Shop", "cart" to "Bag", "account" to "Account").forEach { (route, label) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = { onNavigate(route) },
                        icon = { Text(if (route == "scanner") "📷" else label.take(1)) },
                        label = { Text(label) },
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
private fun LoginScreen(
    service: BhumiveraService,
    onSignedIn: (com.bhumivera.scanner.data.LoginResponse) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var twoFactorRequired by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("BHUMIVERA", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("The Transparent Maison", color = androidx.compose.material3.MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    try {
                        val activity = context.findActivity()
                            ?: throw IllegalStateException("Google sign-in requires an Android activity.")
                        val idToken = requestGoogleIdToken(activity)
                        val response = service.googleLogin(
                            GoogleLoginRequest(idToken = idToken, termsAccepted = termsAccepted),
                        )
                        if (response.requires2FA) {
                            email = response.email.orEmpty()
                            twoFactorRequired = true
                        } else if (response.token.isNullOrBlank()) {
                            error = response.message ?: "Google sign-in did not return a valid session."
                        } else {
                            onSignedIn(response)
                        }
                    } catch (exception: Exception) {
                        error = exception.userMessage()
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && !twoFactorRequired && termsAccepted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Continue with Google")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                enabled = !loading && !twoFactorRequired,
            )
            Text(
                "I agree to the Conditions of Use & Sale and acknowledge the Privacy Notice.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = { uriHandler.openUri("https://bhumivera.com/legal?section=terms") }) {
                Text("Conditions of Use")
            }
            TextButton(onClick = { uriHandler.openUri("https://bhumivera.com/legal?section=privacy") }) {
                Text("Privacy Notice")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f))
            Text("  or use email  ", color = androidx.compose.material3.MaterialTheme.colorScheme.secondary)
            HorizontalDivider(Modifier.weight(1f))
        }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (twoFactorRequired) {
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it.filter(Char::isDigit).take(8); error = null },
                label = { Text("Authenticator code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        } else {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    try {
                        val response = if (twoFactorRequired) {
                            service.verifyTwoFactor(TwoFactorRequest(email.trim(), otp))
                        } else {
                            service.login(LoginRequest(email.trim(), password))
                        }
                        if (response.requires2FA) {
                            twoFactorRequired = true
                        } else if (response.token.isNullOrBlank()) {
                            error = response.message ?: "Sign-in did not return a valid session."
                        } else {
                            onSignedIn(response)
                        }
                    } catch (exception: Exception) {
                        error = exception.userMessage()
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && email.isNotBlank() && if (twoFactorRequired) otp.isNotBlank() else password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(if (twoFactorRequired) "Verify and sign in" else "Sign in")
        }
    }
}

private suspend fun requestGoogleIdToken(activity: Activity): String {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(com.bhumivera.scanner.BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()
    val response = CredentialManager.create(activity).getCredential(
        context = activity,
        request = request,
    )
    val credential = response.credential
    if (credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        throw IllegalStateException("Google did not return a valid sign-in credential.")
    }

    return try {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } catch (exception: GoogleIdTokenParsingException) {
        throw IllegalStateException("Google returned an invalid sign-in credential.", exception)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ShopScreen(
    service: BhumiveraService,
    contentPadding: PaddingValues,
    onProductSelected: (Product) -> Unit,
) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadProducts(search: String?) {
        loading = true
        error = null
        try {
            val response = service.activeProducts(search?.takeIf(String::isNotBlank))
            products = response.data ?: throw IllegalStateException(response.message ?: "The catalog response was empty.")
        } catch (exception: Exception) {
            error = exception.userMessage()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadProducts(null) }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search products") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { scope.launch { loadProducts(query) } },
                enabled = !loading,
            ) { Text("Search") }
        }
        if (loading && products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null && products.isEmpty()) {
            ErrorPanel(error.orEmpty()) { scope.launch { loadProducts(query) } }
        } else {
            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No products found.") }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(product, onClick = { onProductSelected(product) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val imageUrl = product.imageSource()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = product.name,
                modifier = Modifier.size(104.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(product.categoryName ?: product.category ?: "Botanical", color = androidx.compose.material3.MaterialTheme.colorScheme.secondary)
                Text(product.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(product.shortDescription.orEmpty(), maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Text("₹${product.discountPrice ?: product.price ?: 0.0}", color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(
    service: BhumiveraService,
    slug: String,
    contentPadding: PaddingValues,
    onAddedToCart: () -> Unit,
) {
    var product by remember(slug) { mutableStateOf<Product?>(null) }
    var loading by remember(slug) { mutableStateOf(true) }
    var error by remember(slug) { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun loadProduct() {
        loading = true
        error = null
        try {
            val productId = slug.toIntOrNull()
            val response = if (productId != null) {
                service.productById(productId)
            } else {
                service.productBySlug(slug)
            }
            product = response.data ?: throw IllegalStateException(response.message ?: "Product details were not returned.")
        } catch (exception: Exception) {
            error = exception.userMessage()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(slug) { loadProduct() }

    Column(Modifier.fillMaxSize().padding(contentPadding).padding(16.dp)) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> ErrorPanel(error.orEmpty()) { scope.launch { loadProduct() } }
            product != null -> {
                val item = product!!
                AsyncImage(
                    model = item.imageSource(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxWidth().height(280.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(item.categoryName ?: item.category ?: "Botanical", color = androidx.compose.material3.MaterialTheme.colorScheme.secondary)
                Text(item.name, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("₹${item.discountPrice ?: item.price ?: 0.0}", color = androidx.compose.material3.MaterialTheme.colorScheme.primary, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Text(item.description ?: item.shortDescription ?: "Product details are not available.")
                actionMessage?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
                Button(
                    onClick = {
                        scope.launch {
                            actionMessage = null
                            try {
                                service.addToCart(AddCartRequest(item.id, 1))
                                onAddedToCart()
                            } catch (exception: Exception) {
                                actionMessage = exception.userMessage()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) { Text("Add to bag") }
            }
        }
    }
}

@Composable
private fun CartScreen(service: BhumiveraService, contentPadding: PaddingValues) {
    var items by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var total by remember { mutableStateOf(0.0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun refreshCart() {
        loading = true
        error = null
        try {
            val cart = service.cart()
            items = cart.items
            total = cart.total
        } catch (exception: Exception) {
            error = exception.userMessage()
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { refreshCart() }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        when {
            loading && items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null && items.isEmpty() -> ErrorPanel(error.orEmpty()) { scope.launch { refreshCart() } }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Your bag is empty.") }
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.productId }) { item ->
                        CartItemCard(
                            item = item,
                            onQuantityChange = { quantity ->
                                scope.launch {
                                    try {
                                        val updated = service.updateCartItem(item.productId, UpdateCartRequest(quantity))
                                        items = updated.items
                                        total = updated.total
                                    } catch (exception: Exception) {
                                        error = exception.userMessage()
                                    }
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    try {
                                        val updated = service.removeCartItem(item.productId)
                                        items = updated.items
                                        total = updated.total
                                    } catch (exception: Exception) {
                                        error = exception.userMessage()
                                    }
                                }
                            },
                        )
                    }
                }
                error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                Text(
                    text = "Subtotal  ₹${"%.2f".format(total)}",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CartItemCard(item: CartItem, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = resolveImageUrl(item.imageUrl),
                contentDescription = item.name,
                modifier = Modifier.size(76.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text("₹${item.unitPrice ?: 0.0}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) else onRemove() }) { Text("−") }
                    Text(item.quantity.toString())
                    TextButton(onClick = { onQuantityChange(item.quantity + 1) }) { Text("+") }
                    TextButton(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun AccountScreen(
    service: BhumiveraService,
    customer: CustomerUser?,
    sessionStore: SessionStore,
    contentPadding: PaddingValues,
    onSignedOut: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(customer?.name ?: "Signed-in customer", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text(customer?.email.orEmpty(), color = androidx.compose.material3.MaterialTheme.colorScheme.secondary)
        error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    try {
                        service.logout()
                        sessionStore.clear()
                        onSignedOut()
                    } catch (exception: Exception) {
                        error = exception.userMessage()
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Sign out")
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text("Try again") }
    }
}

private fun Product.imageSource(): String? {
    val image = images?.firstOrNull { it.mediaType != "video" }?.let { it.url ?: it.filePath }
        ?: imageUrl
        ?: image
    return resolveImageUrl(image)
}

private fun resolveImageUrl(image: String?): String? {
    val value = image?.trim()?.takeIf(String::isNotEmpty) ?: return null
    if (value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)) return value
    if (value.startsWith("uploads/")) return "${com.bhumivera.scanner.BuildConfig.BACKEND_BASE_URL}/$value"
    return "$IMAGE_BASE_URL/${value.trimStart('/')}"
}

private fun Exception.userMessage(): String = when (this) {
    is NoCredentialException ->
        "No Google account is available on this device. Add a Google account in Android Settings and try again."
    is HttpException -> {
        val serverMessage = response()?.errorBody()?.string()
            ?.let { body ->
                runCatching {
                    com.google.gson.JsonParser.parseString(body)
                        .asJsonObject.get("message")?.asString
                }.getOrNull()
            }
        serverMessage ?: "The request failed (${code()}). Please try again."
    }
    is IOException -> "Could not connect to Bhumivera. Check your internet connection and try again."
    else -> message ?: "Something went wrong. Please try again."
}
