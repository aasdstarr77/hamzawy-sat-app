package com.hamzawy.sat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Navy = Color(0xFF0B1F3A)
private val Yellow = Color(0xFFFFC107)

data class Service(
    val id: Int,
    val title: String,
    val icon: String,
    val description: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HamzawySatApp()
        }
    }
}

@Composable
fun HamzawySatApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            secondary = Yellow
        )
    ) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            AppRouter()
        }
    }
}

@Composable
fun AppRouter() {
    var mode by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<Service?>(null) }

    when (mode) {

        "tech" -> TechnicianLogin(
            onBack = {
                mode = "home"
            },
            onLoggedIn = {
                mode = "techHome"
            }
        )

        "techHome" -> TechnicianHome(
            user = ApiClient.currentUser
                ?: ApiClient.ApiUser(
                    0,
                    "الفني",
                    "",
                    "technician"
                ),
            onLogout = {
                ApiClient.token = null
                ApiClient.currentUser = null
                mode = "home"
            }
        )

        "request" -> selected?.let { service ->
            ServiceRequestScreen(
                service = service,
                onBack = {
                    mode = "home"
                },
                onSubmitted = {
                    mode = "home"
                }
            )
        }

        else -> HomeScreen(
            onTechnician = {
                mode = "tech"
            },
            onService = {
                selected = it
                mode = "request"
            }
        )
    }
}

@Composable
fun HomeScreen(
    onTechnician: () -> Unit,
    onService: (Service) -> Unit
) {
    var services by remember {
        mutableStateOf<List<Service>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                services = ApiClient.services().mapIndexed { i, s ->
                    Service(
                        s.id,
                        s.name,
                        listOf("📡", "📹", "📺")[i % 3],
                        s.description
                    )
                }
            } catch (e: Exception) {
                error = e.message
            }

            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "حمزاوي سات",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "خدمات فنية في مكانك",
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Text(
                    "اختار الخدمة اللي محتاجها",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    "من غير تسجيل أو حساب للعميل",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            error?.let { msg ->
                item {
                    Text(
                        "تعذر الاتصال بالسيرفر: $msg",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            items(services) { service ->
                ServiceCard(service) {
                    onService(service)
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                OutlinedButton(
                    onClick = onTechnician,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("دخول / تسجيل الفني")
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    "سعر الخدمة يتم الاتفاق عليه مع العميل ويحدده الأدمن.",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ServiceCard(
    service: Service,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7F8FA)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                service.icon,
                fontSize = 36.sp
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    service.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Text(
                    service.description,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun ServiceRequestScreen(
    service: Service,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    var area by remember {
        mutableStateOf("")
    }

    var address by remember {
        mutableStateOf("")
    }

    var problem by remember {
        mutableStateOf("")
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("طلب خدمة")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                service.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "مش محتاج تعمل حساب. اكتب بياناتك وإحنا هنتواصل معاك.",
                color = Color.Gray
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = {
                    Text("الاسم")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                },
                label = {
                    Text("رقم الموبايل")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = area,
                onValueChange = {
                    area = it
                },
                label = {
                    Text("المنطقة")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                },
                label = {
                    Text("العنوان بالتفصيل")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = problem,
                onValueChange = {
                    problem = it
                },
                label = {
                    Text("وصف المشكلة")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            message?.let {
                Text(
                    it,
                    color = if (it.startsWith("تم")) {
                        Color(0xFF18794E)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Button(
                onClick = {
                    busy = true

                    scope.launch {
                        try {

                            val orderId = withContext(Dispatchers.IO) {
                                ApiClient.createGuestOrder(
                                    service.id,
                                    name,
                                    phone,
                                    area,
                                    address,
                                    problem
                                )
                            }

                            message =
                                "تم إرسال طلبك بنجاح رقم #$orderId، وهنراجع الطلب ونتواصل معاك لتأكيد السعر."

                        } catch (e: Exception) {
                            message = e.message
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    name.isNotBlank() &&
                    phone.isNotBlank() &&
                    area.isNotBlank() &&
                    address.isNotBlank() &&
                    !busy
            ) {

                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("إرسال طلب الخدمة")
                }
            }
        }
    }
}

@Composable
fun TechnicianLogin(
    onBack: () -> Unit,
    onLoggedIn: (ApiClient.ApiUser) -> Unit
) {
    var phone by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var name by remember {
        mutableStateOf("")
    }

    var register by remember {
        mutableStateOf(false)
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("منطقة الفنيين")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                if (register) {
                    "تسجيل فني جديد"
                } else {
                    "تسجيل دخول الفني"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "الحساب يحتاج موافقة الأدمن قبل استخدام الطلبات.",
                color = Color.Gray
            )

            if (register) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("الاسم")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                },
                label = {
                    Text("رقم الهاتف")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("كلمة المرور")
                },
                modifier = Modifier.fillMaxWidth()
            )

            message?.let {
                Text(
                    it,
                    color = if (it.contains("تم")) {
                        Color(0xFF18794E)
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Button(
                onClick = {
                    busy = true

                    scope.launch {
                        try {

                            if (register) {

                                message = withContext(Dispatchers.IO) {
                                    ApiClient.registerTechnician(
                                        name,
                                        phone,
                                        password
                                    )
                                }

                                register = false

                            } else {

                                val user = withContext(Dispatchers.IO) {
                                    Api
