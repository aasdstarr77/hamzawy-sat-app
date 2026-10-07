package com.hamzawy.sat

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    // For a phone testing against a PC on the same Wi‑Fi, replace with the PC LAN IP.
    // Example: http://192.168.1.10:3000
    var baseUrl = "var baseUrl = "http://127.0.0.1:3000"
    var token: String? = null
    var currentUser: ApiUser? = null

    private val gson = Gson()

    data class ApiUser(val id: Int, val name: String, val phone: String, val role: String,
                       val approved: Boolean = false, val balance: Double? = null)
    data class LoginResponse(val token: String, val user: ApiUser)
    data class ServiceDto(val id: Int, val name: String, val description: String)
    data class OrderDto(val id: Int, val service: String, val area: String?,
                        val address: String? = null, val problem: String? = null,
                        val customer_price: Double? = null, val technician_fee: Double? = null,
                        val status: String)
    data class OpenedOrder(val ok: Boolean, val order_id: Int, val customer_name: String,
                           val customer_phone: String, val service: String, val area: String?,
                           val customer_price: Double, val technician_fee: Double)
    data class BalanceResponse(val balance: Double)

    private fun request(method: String, path: String, body: String? = null): String {
        val conn = (URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection)
        conn.requestMethod = method
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Content-Type", "application/json")
        token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            val msg = try { gson.fromJson(text, Map::class.java)["error"]?.toString() } catch (_: Exception) { null }
            throw Exception(msg ?: "حدث خطأ في الاتصال ($code)")
        }
        return text
    }

    fun login(phone: String, password: String): ApiUser {
        val json = request("POST", "/api/auth/login", gson.toJson(mapOf("phone" to phone, "password" to password)))
        val result = gson.fromJson(json, LoginResponse::class.java)
        token = result.token
        currentUser = result.user
        return result.user
    }

    fun registerTechnician(name: String, phone: String, password: String): String {
        val json = request("POST", "/api/auth/register",
            gson.toJson(mapOf("name" to name, "phone" to phone, "password" to password, "role" to "technician")))
        return try { gson.fromJson(json, Map::class.java)["message"]?.toString() ?: "تم التسجيل" } catch (_: Exception) { "تم التسجيل" }
    }

    fun registerCustomer(name: String, phone: String, password: String): String {
        val json = request("POST", "/api/auth/register",
            gson.toJson(mapOf("name" to name, "phone" to phone, "password" to password, "role" to "customer")))
        return try { gson.fromJson(json, Map::class.java)["message"]?.toString() ?: "تم إنشاء الحساب" } catch (_: Exception) { "تم إنشاء الحساب" }
    }

    fun services(): List<ServiceDto> {
        val json = request("GET", "/api/services")
        return gson.fromJson(json, object : TypeToken<List<ServiceDto>>() {}.type)
    }

    fun createOrder(serviceId: Int, area: String, address: String, problem: String): Int {
        val json = request("POST", "/api/orders", gson.toJson(mapOf(
            "service_id" to serviceId, "area" to area, "address" to address, "problem" to problem
        )))
        return (gson.fromJson(json, Map::class.java)["order_id"] as Number).toInt()
    }


    fun createGuestOrder(serviceId: Int, name: String, phone: String, area: String, address: String, problem: String): Int {
        val oldToken = token
        token = null
        return try {
            val json = request("POST", "/api/orders", gson.toJson(mapOf(
                "service_id" to serviceId, "name" to name, "phone" to phone,
                "area" to area, "address" to address, "problem" to problem
            )))
            (gson.fromJson(json, Map::class.java)["order_id"] as Number).toInt()
        } finally { token = oldToken }
    }

    fun technicianOrders(): List<OrderDto> {
        val json = request("GET", "/api/technician/orders")
        return gson.fromJson(json, object : TypeToken<List<OrderDto>>() {}.type)
    }

    fun balance(): Double = gson.fromJson(request("GET", "/api/technician/balance"), BalanceResponse::class.java).balance

    fun openOrder(id: Int): OpenedOrder =
        gson.fromJson(request("POST", "/api/technician/orders/$id/open", "{}"), OpenedOrder::class.java)
}
