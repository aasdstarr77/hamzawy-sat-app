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

data class Service(val id: Int, val title: String, val icon: String, val description: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HamzawySatApp() }
    }
}

@Composable
fun HamzawySatApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Yellow)) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
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
            onBack = { mode = "home" },
            onLoggedIn = { mode = "techHome" }
        )
        "techHome" -> TechnicianHome(
            user = ApiClient.currentUser ?: ApiClient.ApiUser(0, "الفني", "", "technician"),
            onLogout = { ApiClient.token = null; ApiClient.currentUser = null; mode = "home" }
        )
        "request" -> selected?.let { service ->
            ServiceRequestScreen(
                service = service,
                onBack = { mode = "home" },
                onSubmitted = { mode = "home" }
            )
        }
        else -> HomeScreen(
            onTechnician = { mode = "tech" },
            onService = { selected = it; mode = "request" }
        )
    }
}

@Composable
fun HomeScreen(onTechnician: () -> Unit, onService: (Service) -> Unit) {
    var services by remember { mutableStateOf<List<Service>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                services = ApiClient.services().mapIndexed { i, s ->
                    Service(s.id, s.name, listOf("📡","📹","📺")[i % 3], s.description)
                }
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text("حمزاوي سات", fontWeight = FontWeight.Bold); Text("خدمات فنية في مكانك", fontSize = 12.sp) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White)
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("اختار الخدمة اللي محتاجها", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text("من غير تسجيل أو حساب للعميل", color = Color.Gray, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            if (loading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            error?.let { msg -> item { Text("تعذر الاتصال بالسيرفر: $msg", color = MaterialTheme.colorScheme.error) } }
            items(services) { service -> ServiceCard(service) { onService(service) } }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onTechnician, Modifier.fillMaxWidth()) { Text("دخول / تسجيل الفني") }
                Spacer(Modifier.height(8.dp))
                Text("سعر الخدمة يتم الاتفاق عليه مع العميل ويحدده الأدمن.", color = Color.Gray, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ServiceCard(service: Service, onClick: () -> Unit) {
    Card(onClick = onClick, Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))) {
        Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(service.icon, fontSize = 36.sp); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(service.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(service.description, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ServiceRequestScreen(service: Service, onBack: () -> Unit, onSubmitted: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = {
        TopAppBar(title = { Text("طلب خدمة") }, navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White))
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(service.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("مش محتاج تعمل حساب. اكتب بياناتك وإحنا هنتواصل معاك.", color = Color.Gray)
            OutlinedTextField(name, { name = it }, label = { Text("الاسم") }, Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("رقم الموبايل") }, Modifier.fillMaxWidth())
            OutlinedTextField(area, { area = it }, label = { Text("المنطقة") }, Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text("العنوان بالتفصيل") }, Modifier.fillMaxWidth())
            OutlinedTextField(problem, { problem = it }, label = { Text("وصف المشكلة") },
                Modifier.fillMaxWidth().height(120.dp))
            message?.let { Text(it, color = if (it.startsWith("تم")) Color(0xFF18794E) else MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val orderId = withContext(Dispatchers.IO) {
                                ApiClient.createGuestOrder(service.id, name, phone, area, address, problem)
                            }
                            message = "تم إرسال طلبك بنجاح رقم #$orderId، وهنراجع الطلب ونتواصل معاك لتأكيد السعر."
                        } catch (e: Exception) { message = e.message }
                        finally { busy = false }
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && phone.isNotBlank() && area.isNotBlank() && address.isNotBlank() && !busy
            ) { if (busy) CircularProgressIndicator(Modifier.size(20.dp)) else Text("إرسال طلب الخدمة") }
        }
    }
}

@Composable
fun TechnicianLogin(onBack: () -> Unit, onLoggedIn: (ApiClient.ApiUser) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var register by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = {
        TopAppBar(title = { Text("منطقة الفنيين") }, navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White))
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (register) "تسجيل فني جديد" else "تسجيل دخول الفني", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("الحساب يحتاج موافقة الأدمن قبل استخدام الطلبات.", color = Color.Gray)
            if (register) OutlinedTextField(name, { name = it }, label = { Text("الاسم") }, Modifier.fillMaxWidth())
            OutlinedTextField(phone, { phone = it }, label = { Text("رقم الهاتف") }, Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("كلمة المرور") }, Modifier.fillMaxWidth())
            message?.let { Text(it, color = if (it.contains("تم")) Color(0xFF18794E) else MaterialTheme.colorScheme.error) }
            Button(onClick = {
                busy = true
                scope.launch {
                    try {
                        if (register) {
                            message = withContext(Dispatchers.IO) { ApiClient.registerTechnician(name, phone, password) }
                            register = false
                        } else {
                            val user = withContext(Dispatchers.IO) { ApiClient.login(phone, password) }
                            onLoggedIn(user)
                        }
                    } catch (e: Exception) { message = e.message }
                    finally { busy = false }
                }
            }, Modifier.fillMaxWidth(), enabled = !busy && phone.isNotBlank() && password.length >= 6 && (!register || name.isNotBlank())) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp)) else Text(if (register) "تسجيل الفني" else "دخول")
            }
            OutlinedButton(onClick = { register = !register; message = null }, Modifier.fillMaxWidth()) {
                Text(if (register) "عندي حساب بالفعل" else "تسجيل فني جديد")
            }
        }
    }
}

@Composable
fun TechnicianHome(user: ApiClient.ApiUser, onLogout: () -> Unit) {
    var balance by remember { mutableStateOf(user.balance ?: 0.0) }
    var orders by remember { mutableStateOf<List<ApiClient.OrderDto>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<ApiClient.OpenedOrder?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var agreed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            try {
                balance = withContext(Dispatchers.IO) { ApiClient.balance() }
                orders = withContext(Dispatchers.IO) { ApiClient.technicianOrders() }
            } catch (e: Exception) { message = e.message }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("طلبات الفني") },
            actions = { TextButton(onClick = onLogout) { Text("خروج", color = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, titleContentColor = Color.White))
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("الرصيد الحالي", fontWeight = FontWeight.Bold)
                        Text("${balance.toInt()} جنيه", fontSize = 28.sp, color = Navy)
                        if (!user.approved) Text("الحساب في انتظار موافقة الأدمن.", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = { refresh() }, Modifier.fillMaxWidth()) { Text("تحديث الطلبات") } }
            if (loading) item { CircularProgressIndicator() }
            items(orders) { order ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("طلب #${order.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(order.service)
                        Text("المنطقة: ${order.area ?: "-"}")
                        Text("سعر الخدمة: ${(order.customer_price ?: 0).toInt()} جنيه")
                        Text("الخصم عند الفتح: ${(order.technician_fee ?: 0).toInt()} جنيه")
                        if (selectedId == order.id) {
                            Text("⚠️ تنبيه مهم", fontWeight = FontWeight.Bold)
                            Text("برجاء التأكد من تفاصيل الطلب وسعر الخدمة والخصم قبل فتح الطلب. بمجرد الضغط على «فتح الطلب وخصم الرصيد» وخصم الرصيد، لا يجوز طلب استرداد أو رد قيمة الخصم.")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(agreed, { agreed = it })
                                Text("أوافق على الخصم النهائي بعد فتح الطلب")
                            }
                            Button(
                                enabled = agreed,
                                onClick = {
                                    scope.launch {
                                        try {
                                            opened = withContext(Dispatchers.IO) { ApiClient.openOrder(order.id) }
                                            balance = withContext(Dispatchers.IO) { ApiClient.balance() }
                                            orders = withContext(Dispatchers.IO) { ApiClient.technicianOrders() }
                                            selectedId = null; agreed = false
                                        } catch (e: Exception) { message = e.message }
                                    }
                                }, Modifier.fillMaxWidth()
                            ) { Text("فتح الطلب وخصم ${(order.technician_fee ?: 0).toInt()} جنيه") }
                        } else {
                            OutlinedButton(onClick = { selectedId = order.id; agreed = false }, Modifier.fillMaxWidth()) {
                                Text("مراجعة وفتح الطلب")
                            }
                        }
                    }
                }
            }
            opened?.let { o ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("تم فتح الطلب #${o.order_id}", fontWeight = FontWeight.Bold)
                            Text("العميل: ${o.customer_name}")
                            Text("الهاتف: ${o.customer_phone}")
                            Text("الخدمة: ${o.service}")
                            Text("المنطقة: ${o.area ?: "-"}")
                            Text("السعر المتفق عليه: ${o.customer_price.toInt()} جنيه")
                        }
                    }
                }
            }
        }
    }
}
