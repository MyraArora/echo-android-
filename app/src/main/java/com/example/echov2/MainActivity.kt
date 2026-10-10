package com.example.echov2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.sin

// Colors matching design
val EchoBlueHeader = Color(0xFF0066FF)
val EchoDarkBlueButton = Color(0xFF003859)
val EchoInputFieldBorder = Color(0xFF0B3C4D)
val EchoSubtextGray = Color(0xFF6C8793)
val EchoUserBubbleColor = Color(0xFF003859)
val EchoAiBubbleColor = Color(0xFFE8F1FF)
val EchoBgLight = Color(0xFFF4F7FF)

// Placeholders
const val promptQuestionBgUrl = "https://static.wixstatic.com/media/0cbe0e_13594cad56364c2eb39a63e87e8b3ca0~mv2.png/v1/fill/w_412,h_890,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/Echo%20Mobile%20UI%20(12).png"
const val headerRobotImageUrl = "https://static.wixstatic.com/media/0cbe0e_3514bb0897164745bfd2ee85db385309~mv2.png/v1/fill/w_1200,h_676,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/I%20see%20it%20I%20like%20it%20I%20want%20it%20I%20got%20it.png"
const val echoAvatarUrl = "https://static.wixstatic.com/media/0cbe0e_3514bb0897164745bfd2ee85db385309~mv2.png/v1/fill/w_1200,h_676,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/I%20see%20it%20I%20like%20it%20I%20want%20it%20I%20got%20it.png"

// CONFIGURATIONS
const val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
private const val GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"
private const val GROQ_STT_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
private const val GROQ_CHAT_MODEL = "openai/gpt-oss-120b"
private const val GROQ_STT_MODEL = "whisper-large-v3-turbo"

// RETROFIT PIPELINE DATA MODELS & CLIENT
data class ProcessSessionRequest(
    val user_id: String,
    val session_id: String,
    val bucket_name: String = "echo-audio-bucket",
    val gcs_audio_path: String,
    val transcript: String? = null
)

data class ProcessSessionResponse(
    val message: String,
    val status: String
)

interface EchoApiService {
    @POST("api/v1/process-session")
    suspend fun processSession(
        @Body request: ProcessSessionRequest
    ): Response<ProcessSessionResponse>
}

object NetworkClient {
    private const val BASE_URL = "https://echo-backend-main.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: EchoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EchoApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EchoNavigationFlow()
                }
            }
        }
    }
}

@Composable
fun EchoNavigationFlow() {
    var flowStep by remember { mutableIntStateOf(0) }

    val screen1Url = "https://static.wixstatic.com/media/0cbe0e_f8215ad577eb4f619730bb050a85adb7~mv2.png/v1/fill/w_500,h_1082,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/Echo%20Mobile%20UI%20(5).png"
    val swipeImages = listOf(
        "https://static.wixstatic.com/media/0cbe0e_36d0cf0b3a22443cb872540c91b5ffc6~mv2.png/v1/fill/w_378,h_818,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/Echo%20Mobile%20UI%20(6).png",
        "https://static.wixstatic.com/media/0cbe0e_fa522f31386a4d0c92386dccf65b386d~mv2.png/v1/fill/w_500,h_1082,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/Echo%20Mobile%20UI%20(7).png",
        "https://static.wixstatic.com/media/0cbe0e_b3c4311d30c5450081ac2244d0802ca7~mv2.png/v1/fill/w_500,h_1082,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/Echo%20Mobile%20UI%20(8).png"
    )

    when (flowStep) {
        0 -> FullScreenImage(url = screen1Url, onClick = { flowStep = 1 })
        1 -> SwipeSection(imageUrls = swipeImages, onFinishedSwiping = { flowStep = 2 })
        2 -> AccountPromptScreen(
            onYesClicked = { flowStep = 3 },
            onNoClicked = { flowStep = 4 }
        )
        3 -> SignInScreen(
            onNavigateToSignUp = { flowStep = 4 },
            onLoginSuccess = { flowStep = 5 }
        )
        4 -> SignUpScreen(
            onNavigateToSignIn = { flowStep = 3 },
            onSignUpSuccess = { flowStep = 5 }
        )
        5 -> EchoHomeScreen(onOpenChat = { flowStep = 6 })
        6 -> EchoChatScreen(onBackClicked = { flowStep = 5 })
        else -> EchoHomeScreen(onOpenChat = { flowStep = 6 })
    }
}

@Composable
fun FullScreenImage(url: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun SwipeSection(imageUrls: List<String>, onFinishedSwiping: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = if (pagerState.currentPage == imageUrls.size - 1) "Tap to continue" else "Swipe ->",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .clickable {
                    if (pagerState.currentPage == imageUrls.size - 1) {
                        onFinishedSwiping()
                    }
                }
        )
    }
}

@Composable
fun AccountPromptScreen(onYesClicked: () -> Unit, onNoClicked: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = promptQuestionBgUrl,
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp, start = 32.dp, end = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onYesClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EchoDarkBlueButton)
            ) {
                Text(text = "Yes", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Button(
                onClick = onNoClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EchoDarkBlueButton)
            ) {
                Text(text = "No", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
fun SignInScreen(onNavigateToSignUp: () -> Unit, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAgreed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    AuthLayout(headerImage = headerRobotImageUrl) {
        Text(
            text = "Welcome back",
            color = EchoSubtextGray,
            fontSize = 18.sp
        )
        Text(
            text = "Sign in",
            color = Color.Black,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        CustomOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email Address",
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = EchoSubtextGray) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        CustomOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            isPassword = true,
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = EchoSubtextGray) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TermsCheckbox(isAgreed = isAgreed, onCheckedChange = { isAgreed = it })

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!isAgreed) {
                    Toast.makeText(context, "Please accept the Terms & Privacy policy", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Welcome back, we missed you!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Error ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EchoDarkBlueButton),
            enabled = !isLoading
        ) {
            Text(text = if (isLoading) "Signing in" else "Sign in", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSignUp() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Don't Have an Account? ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Sign up")
                    }
                },
                color = Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun SignUpScreen(onNavigateToSignIn: () -> Unit, onSignUpSuccess: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAgreed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    AuthLayout(headerImage = headerRobotImageUrl) {
        Text(
            text = "Welcome",
            color = EchoSubtextGray,
            fontSize = 18.sp
        )
        Text(
            text = "Create an Account",
            color = Color.Black,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        CustomOutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = "Full Name",
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = EchoSubtextGray) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        CustomOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email Address",
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = EchoSubtextGray) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        CustomOutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            isPassword = true,
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = EchoSubtextGray) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TermsCheckbox(isAgreed = isAgreed, onCheckedChange = { isAgreed = it })

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!isAgreed) {
                    Toast.makeText(context, "Please accept the Terms & Privacy policy", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid ?: ""
                            val userMap = hashMapOf(
                                "fullName" to fullName.trim(),
                                "email" to email.trim(),
                                "createdAt" to System.currentTimeMillis()
                            )
                            db.collection("users").document(userId).set(userMap)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                                    onSignUpSuccess()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            isLoading = false
                            Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EchoDarkBlueButton),
            enabled = !isLoading
        ) {
            Text(text = if (isLoading) "Creating..." else "Sign Up", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToSignIn() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Already Have an Account? ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Log in")
                    }
                },
                color = Color.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun AuthLayout(headerImage: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EchoBlueHeader)
    ) {
        AsyncImage(
            model = headerImage,
            contentDescription = "Robot Header",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.38f),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(28.dp)
        ) {
            content()
        }
    }
}

@Composable
fun CustomOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    leadingIcon: @Composable () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .border(1.5.dp, EchoInputFieldBorder, RoundedCornerShape(29.dp)),
        placeholder = { Text(text = placeholder, color = EchoSubtextGray, fontSize = 15.sp) },
        leadingIcon = leadingIcon,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text),
        shape = RoundedCornerShape(29.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
fun TermsCheckbox(isAgreed: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = isAgreed,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = EchoDarkBlueButton)
        )
        Text(
            text = buildAnnotatedString {
                append("I agree to the ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Terms & Privacy")
                }
            },
            fontSize = 13.sp,
            color = Color.DarkGray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoHomeScreen(onOpenChat: () -> Unit = {}) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf("User") }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fetchedName = document.getString("fullName")
                        if (!fetchedName.isNullOrEmpty()) {
                            userName = fetchedName
                        }
                    }
                }
        }
    }

    Scaffold(
        containerColor = EchoBgLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = EchoBlueHeader,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Welcome back,",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = userName,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HeaderIconButtonUrl(
                                imageUrl = "https://cdn-icons-png.flaticon.com/512/1436/1436627.png",
                                onClick = { Toast.makeText(context, "Conversation History Clicked", Toast.LENGTH_SHORT).show() }
                            )
                            HeaderIconButtonUrl(
                                imageUrl = "https://cdn-icons-png.flaticon.com/512/1827/1827301.png",
                                onClick = { Toast.makeText(context, "Notifications Clicked", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            placeholder = { Text("Search Here", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            enabled = false
                        )

                        HeaderIconButtonUrl(
                            imageUrl = "https://cdn-icons-png.magnific.com/256/771/771203.png?semt=ais_white_label",
                            onClick = { Toast.makeText(context, "Settings Clicked", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AsyncImage(
                    model = "https://static.wixstatic.com/media/0cbe0e_f460b76d704b4f40863e8547ed75060b~mv2.png/v1/fill/w_787,h_302,al_c,lg_1,q_85,enc_avif,quality_auto/0cbe0e_f460b76d704b4f40863e8547ed75060b~mv2.png",
                    contentDescription = "Hey There! Wanna chat?",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenChat() },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureGridItemUrl(
                            modifier = Modifier.weight(1f),
                            iconUrl = "https://cdn-icons-png.flaticon.com/512/7708/7708371.png",
                            title = "Games",
                            onClick = { Toast.makeText(context, "Games Clicked", Toast.LENGTH_SHORT).show() }
                        )
                        FeatureGridItemUrl(
                            modifier = Modifier.weight(1f),
                            iconUrl = "https://static.thenounproject.com/png/1275974-200.png",
                            title = "Clubhouse",
                            onClick = { Toast.makeText(context, "Clubhouse Clicked", Toast.LENGTH_SHORT).show() }
                        )
                        FeatureGridItemUrl(
                            modifier = Modifier.weight(1f),
                            iconUrl = "https://assets.streamlinehq.com/image/private/w_300,h_300,ar_1/f_auto/v1/icons/business-payments/leaderboard-c2xgbi34v5lkfqcsnn4rls.png/leaderboard-3rvm6vbkyzgye3ova2wwy.png?_a=DATAiZAAZAA0",
                            title = "Leaderboard",
                            onClick = { Toast.makeText(context, "Leaderboard Clicked", Toast.LENGTH_SHORT).show() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureGridItemUrl(
                            modifier = Modifier.weight(1f),
                            iconUrl = "https://cdn-icons-png.flaticon.com/512/9259/9259956.png",
                            title = "Reminders",
                            onClick = { Toast.makeText(context, "Reminders Clicked", Toast.LENGTH_SHORT).show() }
                        )
                        FeatureGridItemUrl(
                            modifier = Modifier.weight(1f),
                            iconUrl = "https://static.thenounproject.com/png/3203474-200.png",
                            title = "Stories",
                            onClick = { Toast.makeText(context, "Stories Clicked", Toast.LENGTH_SHORT).show() }
                        )
                        FeatureGridItemUrl(
                            modifier = Modifier.weight(1f),
                            iconUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSu94P3xVarIOEwDyxZy11BBAP9ZkelKbetT3C0gi424TnO68nsRQAb50w&s=10",
                            title = "Jokes",
                            onClick = { Toast.makeText(context, "Jokes Clicked", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val quoteImageUrls = listOf(
                    "https://static.wixstatic.com/media/0cbe0e_c2e41586762448feb628d27bc66cfdcb~mv2.png/v1/fill/w_424,h_240,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/0cbe0e_c2e41586762448feb628d27bc66cfdcb~mv2.png",
                    "https://static.wixstatic.com/media/0cbe0e_6789842d789d4f4faae4d78babd083cd~mv2.png/v1/fill/w_424,h_240,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/0cbe0e_6789842d789d4f4faae4d78babd083cd~mv2.png",
                    "https://static.wixstatic.com/media/0cbe0e_2050ea3787c349a4ae5950d80921b007~mv2.png/v1/fill/w_424,h_240,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/0cbe0e_2050ea3787c349a4ae5950d80921b007~mv2.png",
                    "https://static.wixstatic.com/media/0cbe0e_fcd95cd343b4468ab5bf92a0a8e3b495~mv2.png/v1/fill/w_424,h_240,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/0cbe0e_fcd95cd343b4468ab5bf92a0a8e3b495~mv2.png"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quoteImageUrls) { quoteUrl ->
                        AsyncImage(
                            model = quoteUrl,
                            contentDescription = "Quote Card",
                            modifier = Modifier
                                .width(300.dp)
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// SCREEN 5: CHAT & VOICE SYSTEM WITH DIRECT GROQ WHISPER STT & RENDER BACKEND
// ==========================================

enum class ChatMode { CHAT, VOICE }

data class Message(
    val id: String = System.currentTimeMillis().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoChatScreen(onBackClicked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeMode by remember { mutableStateOf(ChatMode.VOICE) }
    var inputText by remember { mutableStateOf("") }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isGeneratingResponse by remember { mutableStateOf(false) }

    val savedGlobalFacts = remember { mutableStateListOf<String>() }

    val messages = remember {
        mutableStateListOf(
            Message(
                text = "Hello! I'm Echo, your AI Companion. How can I brighten your day today?",
                isUser = false,
                timestamp = "10:00 AM"
            )
        )
    }

    val listState = rememberLazyListState()

    // --- MEDIARECORDER AUDIO CAPTURE ---
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }

    fun startAudioRecording(): File? {
        return try {
            val audioFile = File(context.cacheDir, "session_audio_${System.currentTimeMillis()}.m4a")
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            currentAudioFile = audioFile
            audioFile
        } catch (e: Exception) {
            android.util.Log.e("EchoAudio", "Error starting MediaRecorder", e)
            null
        }
    }

    fun stopAudioRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val savedFile = currentAudioFile
            currentAudioFile = null
            savedFile
        } catch (e: Exception) {
            android.util.Log.e("EchoAudio", "Error stopping MediaRecorder", e)
            null
        }
    }

    // --- NATIVE TEXT TO SPEECH ---
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        lateinit var tts: TextToSpeech

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val languageResult = tts.setLanguage(Locale.US)

                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    ttsReady = false
                    android.util.Log.e("EchoTTS", "English TTS language is missing or unsupported")
                } else {
                    ttsEngine = tts
                    ttsReady = true

                    tts.setOnUtteranceProgressListener(
                        object : android.speech.tts.UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) { isSpeaking = true }
                            override fun onDone(utteranceId: String?) { isSpeaking = false }
                            override fun onError(utteranceId: String?) { isSpeaking = false }
                        }
                    )
                }
            } else {
                ttsReady = false
            }
        }

        onDispose {
            tts.stop()
            tts.shutdown()
            ttsEngine = null
            ttsReady = false
        }
    }

    fun speakOutLoud(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val tts = ttsEngine
        if (tts == null || !ttsReady) return
        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "EchoResponse")
    }

    // Load User Facts from Firestore
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val facts = doc.get("globalFacts") as? List<*>
                        facts?.filterIsInstance<String>()?.let {
                            savedGlobalFacts.addAll(it)
                        }
                    }
                }
        }
    }

    fun extractAndSaveGlobalFacts() {
        if (messages.size <= 1) return
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val conversationSnapshot = messages.toList()

        scope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val fullConversation = conversationSnapshot.joinToString("\n") {
                    val sender = if (it.isUser) "User" else "Echo"
                    "$sender: ${it.text}"
                }

                val prompt = """
                    Analyze this conversation between a user and Echo.
                    Extract only useful, reasonably stable facts or preferences explicitly stated by the user.
                    Do not infer sensitive traits. Return only bullet points, each starting with '* '.
                    If there are no new meaningful facts, return exactly NONE.

                    Conversation:
                    $fullConversation
                """.trimIndent()

                connection = (URL(GROQ_CHAT_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("Authorization", "Bearer $GROQ_API_KEY")
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                }

                val payload = JSONObject().apply {
                    put("model", GROQ_CHAT_MODEL)
                    put("messages", JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content",
                            "Extract stable user facts only when explicitly stated. Return bullet points beginning with * or exactly NONE. Do not infer sensitive traits."))
                        put(JSONObject().put("role", "user").put("content", prompt))
                    })
                    put("temperature", 0.1)
                    put("max_tokens", 300)
                }

                connection!!.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection!!.responseCode
                val responseBody = (if (status in 200..299) connection!!.inputStream else connection!!.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (status in 200..299) {
                    val rawText = JSONObject(responseBody)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .optString("content", "")
                        .trim()

                    if (rawText.isNotBlank() && !rawText.equals("NONE", ignoreCase = true)) {
                        val bulletPoints = rawText.lines()
                            .map { it.trim().removePrefix("*").removePrefix("-").trim() }
                            .filter { it.isNotBlank() && !it.equals("NONE", ignoreCase = true) }
                            .distinct()

                        if (bulletPoints.isNotEmpty()) {
                            FirebaseFirestore.getInstance().collection("users").document(userUid)
                                .update("globalFacts", FieldValue.arrayUnion(*bulletPoints.toTypedArray()))
                            withContext(Dispatchers.Main) {
                                bulletPoints.forEach { fact -> if (!savedGlobalFacts.contains(fact)) savedGlobalFacts.add(fact) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EchoGroqFacts", "Error extracting global facts", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun generateGroqResponse(userPrompt: String) {
        if (isGeneratingResponse) return
        isGeneratingResponse = true
        val conversationSnapshot = messages.toList()
        val factsSnapshot = savedGlobalFacts.toList()

        scope.launch(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(GROQ_CHAT_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 60000
                    setRequestProperty("Authorization", "Bearer $GROQ_API_KEY")
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                }

                val apiMessages = JSONArray()
                apiMessages.put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are Echo, a polite, warm, and supportive AI companion. Keep each response under 120 words without markdown or emojis.")
                })

                if (factsSnapshot.isNotEmpty()) {
                    apiMessages.put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Facts previously shared:\n" + factsSnapshot.joinToString("\n") { "- $it" })
                    })
                }

                conversationSnapshot.forEach { msg ->
                    apiMessages.put(JSONObject().apply {
                        put("role", if (msg.isUser) "user" else "assistant")
                        put("content", msg.text)
                    })
                }

                val payload = JSONObject().apply {
                    put("model", GROQ_CHAT_MODEL)
                    put("messages", apiMessages)
                    put("temperature", 0.7)
                    put("max_tokens", 300)
                }

                connection!!.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection!!.responseCode
                val responseBody = (if (status in 200..299) connection!!.inputStream else connection!!.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (status in 200..299) {
                    val aiReply = JSONObject(responseBody)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .optString("content", "")
                        .trim()

                    withContext(Dispatchers.Main) {
                        if (aiReply.isNotBlank()) {
                            messages.add(Message(text = aiReply, isUser = false, timestamp = "Just now"))
                            speakOutLoud(aiReply)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EchoGroq", "Groq chat request failed", e)
            } finally {
                connection?.disconnect()
                withContext(Dispatchers.Main) { isGeneratingResponse = false }
            }
        }
    }

    // --- DIRECT GROQ STT WHISPER-LARGE-V3-TURBO TRANSCRIPTION ---
    suspend fun transcribeAudioWithGroq(audioFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", GROQ_STT_MODEL)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(GROQ_STT_URL)
                .addHeader("Authorization", "Bearer $GROQ_API_KEY")
                .post(requestBody)
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()

            if (response.isSuccessful && responseText.isNotBlank()) {
                val json = JSONObject(responseText)
                json.optString("text", "").trim()
            } else {
                android.util.Log.e("EchoGroqSTT", "Groq STT error: $responseText")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("EchoGroqSTT", "Error during Groq STT call", e)
            null
        }
    }

    // --- TRIGGER BACKEND PIPELINE (UPLOAD AUDIO & STUB RECORD) ---
    fun processVoiceSession(audioFile: File) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val sessionId = "session_${System.currentTimeMillis()}"
        val gcsPath = "users/$userId/sessions/$sessionId/audio.m4a"

        scope.launch(Dispatchers.IO) {
            // 1. Run direct Kotlin Groq Whisper STT
            val transcript = transcribeAudioWithGroq(audioFile) ?: ""

            withContext(Dispatchers.Main) {
                if (transcript.isNotBlank()) {
                    messages.add(Message(text = transcript, isUser = true, timestamp = "Just now"))
                    generateGroqResponse(transcript)
                } else {
                    Toast.makeText(context, "Could not process audio. Try again.", Toast.LENGTH_SHORT).show()
                }
            }

            // 2. Persist initial Firestore session stub
            val sessionRef = FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("sessions").document(sessionId)

            val initialSessionData = hashMapOf(
                "timestamp" to FieldValue.serverTimestamp(),
                "status" to "PENDING",
                "audio_gcs_path" to gcsPath,
                "raw_transcript" to transcript
            )

            sessionRef.set(initialSessionData).addOnSuccessListener {
                // 3. Upload Audio to Firebase Storage
                val storageRef = FirebaseStorage.getInstance().reference.child(gcsPath)
                storageRef.putFile(Uri.fromFile(audioFile)).addOnSuccessListener {

                    // 4. Trigger Render Backend Pipeline with supplied transcript
                    scope.launch(Dispatchers.IO) {
                        try {
                            val request = ProcessSessionRequest(
                                user_id = userId,
                                session_id = sessionId,
                                bucket_name = "echo-audio-bucket",
                                gcs_audio_path = gcsPath,
                                transcript = transcript
                            )
                            val response = NetworkClient.apiService.processSession(request)
                            if (response.isSuccessful) {
                                android.util.Log.d("EchoPipeline", "Triggered backend for session $sessionId")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("EchoPipeline", "Failed to call Render API", e)
                        }
                    }
                }
            }
        }
    }

    // --- PERMISSIONS LAUNCHER ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val startedFile = startAudioRecording()
            if (startedFile != null) {
                isRecordingAudio = true
            }
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleRecordButtonClick() {
        if (isRecordingAudio) {
            // Stop recording
            isRecordingAudio = false
            val audioFile = stopAudioRecording()
            if (audioFile != null && audioFile.exists()) {
                Toast.makeText(context, "Listening...", Toast.LENGTH_SHORT).show()
                processVoiceSession(audioFile)
            }
        } else {
            // Start recording
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                ttsEngine?.stop()
                isSpeaking = false
                val startedFile = startAudioRecording()
                if (startedFile != null) {
                    isRecordingAudio = true
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Scaffold(
        containerColor = EchoBgLight,
        topBar = {
            Surface(
                color = EchoBlueHeader,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            extractAndSaveGlobalFacts()
                            onBackClicked()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Box {
                            AsyncImage(
                                model = echoAvatarUrl,
                                contentDescription = "Echo Avatar",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .align(Alignment.BottomEnd)
                                    .border(1.5.dp, EchoBlueHeader, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Echo",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = if (isGeneratingResponse) "Thinking..." else if (activeMode == ChatMode.CHAT) "Online • Ready to chat" else "Voice Mode Active",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = Color.White
                            )
                        }
                    }

                    ModeToggleBar(
                        activeMode = activeMode,
                        onModeSelected = { activeMode = it }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeMode) {
                ChatMode.CHAT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages) { message ->
                                ChatBubbleItem(message = message)
                            }
                        }

                        ChatInputBar(
                            value = inputText,
                            onValueChange = { inputText = it },
                            onSend = {
                                val cleanText = inputText.trim()
                                if (cleanText.isNotBlank()) {
                                    messages.add(
                                        Message(
                                            text = cleanText,
                                            isUser = true,
                                            timestamp = "Just now"
                                        )
                                    )
                                    inputText = ""
                                    generateGroqResponse(cleanText)
                                }
                            },
                            onMicClicked = {
                                activeMode = ChatMode.VOICE
                            }
                        )
                    }
                }

                ChatMode.VOICE -> {
                    VoiceModeScreen(
                        isRecording = isRecordingAudio,
                        isSpeaking = isSpeaking,
                        onRecordClick = { handleRecordButtonClick() },
                        onSwitchToChat = { activeMode = ChatMode.CHAT }
                    )
                }
            }
        }
    }
}

@Composable
fun ModeToggleBar(
    activeMode: ChatMode,
    onModeSelected: (ChatMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val selectedBg = EchoDarkBlueButton
        val unselectedBg = Color.Transparent

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (activeMode == ChatMode.CHAT) selectedBg else unselectedBg)
                .clickable { onModeSelected(ChatMode.CHAT) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💬 Chat Mode",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (activeMode == ChatMode.CHAT) FontWeight.Bold else FontWeight.Normal
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (activeMode == ChatMode.VOICE) selectedBg else unselectedBg)
                .clickable { onModeSelected(ChatMode.VOICE) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎙️ Voice Mode",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (activeMode == ChatMode.VOICE) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun ChatBubbleItem(message: Message) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) EchoUserBubbleColor else EchoAiBubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else Color(0xFF1E293B),
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.timestamp,
                    color = if (isUser) Color.White.copy(alpha = 0.6f) else EchoSubtextGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClicked: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onMicClicked,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EchoBgLight)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Switch to Voice",
                    tint = EchoDarkBlueButton
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Type a message...", color = EchoSubtextGray, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 100.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EchoInputFieldBorder,
                    unfocusedBorderColor = EchoInputFieldBorder.copy(alpha = 0.4f),
                    focusedContainerColor = EchoBgLight,
                    unfocusedContainerColor = EchoBgLight
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                singleLine = false,
                maxLines = 3
            )

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank()) EchoDarkBlueButton else EchoSubtextGray.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun AudioWaveformAnimation(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = EchoBlueHeader
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(modifier = modifier) {
        val barCount = 11
        val barWidth = 4.dp.toPx()
        val spacing = 10.dp.toPx()
        val totalWidth = barCount * barWidth + (barCount - 1) * spacing
        val startX = (size.width - totalWidth) / 2
        val maxHeight = size.height
        val minHeight = 8.dp.toPx()

        for (i in 0 until barCount) {
            val barX = startX + i * (barWidth + spacing)

            val currentBarHeight = if (isActive) {
                val offsetPhase = phase + (i * 0.6f)
                val waveMultiplier = (sin(offsetPhase.toDouble()).toFloat() + 1f) / 2f
                minHeight + (maxHeight - minHeight) * waveMultiplier
            } else {
                minHeight
            }

            val barY = (size.height - currentBarHeight) / 2

            drawRoundRect(
                color = if (isActive) barColor else barColor.copy(alpha = 0.3f),
                topLeft = Offset(x = barX, y = barY),
                size = Size(width = barWidth, height = currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

@Composable
fun VoiceModeScreen(
    isRecording: Boolean,
    isSpeaking: Boolean,
    onRecordClick: () -> Unit,
    onSwitchToChat: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording || isSpeaking) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    isRecording -> "Recording audio..."
                    isSpeaking -> "Echo is speaking..."
                    else -> "Tap the mic & speak"
                },
                color = EchoDarkBlueButton,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isRecording) "Recording your voice for analysis. Tap stop when finished." else "Ask Echo anything or just chat",
                color = EchoSubtextGray,
                fontSize = 14.sp
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(EchoBlueHeader.copy(alpha = if (isRecording || isSpeaking) 0.25f else 0.1f))
            )

            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = EchoBlueHeader),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .size(120.dp)
                    .clickable { onRecordClick() }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = echoAvatarUrl,
                        contentDescription = "Echo Avatar",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        AudioWaveformAnimation(
            isActive = isRecording || isSpeaking,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.Red.copy(alpha = 0.1f) else Color.White)
                        .border(1.dp, EchoInputFieldBorder.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color.Red else EchoDarkBlueButton
                    )
                }

                Button(
                    onClick = onRecordClick,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFE53935) else EchoDarkBlueButton
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Tap to Record",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, EchoInputFieldBorder.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaker",
                        tint = EchoDarkBlueButton
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSwitchToChat) {
                Text(
                    text = "Switch back to text chat",
                    color = EchoBlueHeader,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HeaderIconButtonUrl(imageUrl: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun FeatureGridItemUrl(
    modifier: Modifier = Modifier,
    iconUrl: String,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = iconUrl,
                contentDescription = title,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }
    }
}