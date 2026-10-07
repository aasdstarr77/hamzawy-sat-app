package com.hamzawy.sat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var register by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("منطقة الفنيين") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
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
                text = if (register) "تسجيل فني جديد" else "تسجيل دخول الفني",
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
                    onValueChange = { name = it },
                    label = { Text("الاسم") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                modifier = Modifier.fillMaxWidth()
            )

            message?.let { msg ->
                Text(
                    text = msg,
                    color = if (msg.contains("تم")) {
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
                                        name = name,
                                        phone = phone,
                                        password = password
                                    )
                                }
                                register = false
                            } else {
                                val user = withContext(Dispatchers.IO) {
                                    ApiClient.login(
                                        phone = phone,
                                        password = password
                                    )
                                }

                                ApiClient.currentUser = user

                                if (!user.approved) {
                                    message = "الحساب لسه مستني موافقة الأدمن."
                                } else {
                                    onLoggedIn(user)
                                }
                            }
                        } catch (e: Exception) {
                            message = e.message ?: "حدث خطأ أثناء تسجيل الدخول"
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = phone.isNotBlank() &&
                    password.isNotBlank() &&
                    !busy
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(if (register) "تسجيل الفني" else "دخول")
                }
            }

            TextButton(
                onClick = {
                    register = !register
                    message = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (register) {
                        "عندي حساب بالفعل - تسجيل الدخول"
                    } else {
                        "أنا فني جديد - إنشاء حساب"
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianHome(
    user: ApiClient.ApiUser,
    onLogout: () -> Unit
) {
    var balance by remember {
        mutableStateOf(user.balance ?: 0.0)
    }

    var orders by remember {
        mutableStateOf<List<ApiClient.OrderDto>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    var opened by remember {
        mutableStateOf<ApiClient.OpenedOrder?>(null)
    }

    var selectedOrder by remember {
        mutableStateOf<ApiClient.OrderDto?>(null)
    }

    var agree by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            loading = true

            try {
                val result = withContext(Dispatchers.IO) {
                    ApiClient.technicianOrders()
                }

                orders = result

                balance = withContext(Dispatchers.IO) {
                    ApiClient.balance()
                }

                message = null

            } catch (e: Exception) {
                message = e.message ?: "تعذر تحميل البيانات"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("لوحة الفني")
                },
                actions = {
                    TextButton(
                        onClick = onLogout
                    ) {
                        Text(
                            "خروج",
                            color = Color.White
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
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "الرصيد المتاح",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "${balance.toInt()} جنيه",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                    }
                }
            }

            item {
                Text(
                    "الطلبات المتاحة",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
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

            message?.let { msg ->
                item {
                    Text(
                        msg,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!loading && orders.isEmpty()) {
                item {
                    Text(
                        "لا توجد طلبات متاحة حاليًا.",
                        color = Color.Gray
                    )
                }
            }

            items(orders) { order ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {

                        Text(
                            order.service,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "المنطقة: ${order.area ?: "-"}"
                        )

                        Text(
                            "السعر المتفق عليه: ${(order.customer_price ?: 0.0).toInt()} جنيه"
                        )

                        Text(
                            "خصم فتح الطلب: ${(order.technician_fee ?: 0.0).toInt()} جنيه",
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Button(
                            onClick = {
                                selectedOrder = order
                                agree = false
                                message = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "فتح الطلب وخصم ${(order.technician_fee ?: 0.0).toInt()} جنيه"
                            )
                        }
                    }
                }
            }

            if (opened != null) {
                item {
                    val o = opened!!

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {

                            Text(
                                "تم فتح الطلب #${o.order_id}",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "العميل: ${o.customer_name}"
                            )

                            Text(
                                "الهاتف: ${o.customer_phone}"
                            )

                            Text(
                                "الخدمة: ${o.service}"
                            )

                            Text(
                                "المنطقة: ${o.area ?: "-"}"
                            )

                            Text(
                                "السعر المتفق عليه: ${o.customer_price.toInt()} جنيه"
                            )
                        }
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->

        AlertDialog(
            onDismissRequest = {
                selectedOrder = null
            },

            title = {
                Text("تنبيه مهم")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        "برجاء التأكد من تفاصيل الطلب وسعر الخدمة والخصم قبل فتح الطلب."
                    )

                    Text(
                        "بمجرد الضغط على «فتح الطلب وخصم الرصيد» وخصم الرصيد، لا يجوز طلب استرداد أو رد قيمة الخصم.",
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = agree,
                            onCheckedChange = {
                                agree = it
                            }
                        )

                        Text(
                            "أوافق على خصم قيمة فتح الطلب ولا أطلب استردادها."
                        )
                    }
                }
            },

            confirmButton = {

                Button(
                    enabled = agree,
                    onClick = {

                        scope.launch {

                            try {

                                val result = withContext(Dispatchers.IO) {
                                    ApiClient.openOrder(order.id)
                                }

                                opened = result

                                balance = withContext(Dispatchers.IO) {
                                    ApiClient.balance()
                                }

                                orders = withContext(Dispatchers.IO) {
                                    ApiClient.technicianOrders()
                                }

                                selectedOrder = null

                                message = "تم فتح الطلب وخصم الرصيد بنجاح."

                            } catch (e: Exception) {

                                message =
                                    e.message ?: "تعذر فتح الطلب."
                            }
                        }
                    }
                ) {
                    Text(
                        "فتح الطلب وخصم ${(order.technician_fee ?: 0.0).toInt()} جنيه"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        selectedOrder = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
