import com.example.safemiles.ApiResponse
import com.example.safemiles.ChatbotRequest
import com.example.safemiles.ChatbotResponse
import com.example.safemiles.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST


interface ApiiService {

    @POST("/add_user")
    fun registerUser(@Body user: User): Call<ApiResponse>

    @POST("/login_user")
    fun loginUser(@Body user: User): Call<ApiResponse>

    @GET("/get_users")
    fun getUsers(): Call<List<User>>

    @POST("chatbot/message")
    fun sendMessage(@Body request: ChatbotRequest): Call<ChatbotResponse>
}


