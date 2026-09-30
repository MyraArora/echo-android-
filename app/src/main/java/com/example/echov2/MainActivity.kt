package com.example.echov2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Colors matching the design
val EchoBlueHeader = Color(0xFF0066FF)
val EchoDarkBlueButton = Color(0xFF003859)
val EchoGoogleBlue = Color(0xFF0F6EFF)
val EchoInputFieldBorder = Color(0xFF0B3C4D)
val EchoSubtextGray = Color(0xFF6C8793)

// Image URL Placeholders
const val promptQuestionBgUrl = "https://static.wixstatic.com/media/0cbe0e_13594cad56364c2eb39a63e87e8b3ca0~mv2.png/v1/fill/w_412,h_890,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/Echo%20Mobile%20UI%20(12).png"
const val headerRobotImageUrl = "https://static.wixstatic.com/media/0cbe0e_3514bb0897164745bfd2ee85db385309~mv2.png/v1/fill/w_1200,h_676,al_c,q_90,usm_0.66_1.00_0.01,enc_avif,quality_auto/I%20see%20it%20I%20like%20it%20I%20want%20it%20I%20got%20it.png"
const val googleLogoUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c1/Google_%22G%22_logo.svg/3840px-Google_%22G%22_logo.svg.png?utm_source=commons.wikimedia.org&utm_campaign=index&utm_content=thumbnail"

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
    var flowStep by remember { mutableStateOf(0) }

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
        else -> EchoHomeScreen()
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

// SCREEN 1: Fullscreen Prompt Screen with Yes/No Buttons
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

// SCREEN 2: Sign In Page
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
            placeholder = "Email Adress",
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
                            Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
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
            Text(text = if (isLoading) "Signing in..." else "Sign in", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        GoogleSignInButton(text = "Sign Up with Google")

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

// SCREEN 3: Create Account Page
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
            placeholder = "Email Adress",
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

        Spacer(modifier = Modifier.height(12.dp))

        GoogleSignInButton(text = "Sign Up with Google")

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

// REUSABLE COMPONENTS

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

@Composable
fun GoogleSignInButton(text: String) {
    Button(
        onClick = { /* Google Auth integration target */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EchoGoogleBlue)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = googleLogoUrl,
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun EchoHomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome to Echo Home!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = EchoDarkBlueButton
        )
    }
}