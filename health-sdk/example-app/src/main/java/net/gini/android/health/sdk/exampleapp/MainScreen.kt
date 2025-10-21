package net.gini.android.health.sdk.exampleapp

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.gini.android.health.sdk.BuildConfig
import net.gini.android.health.sdk.exampleapp.review.ReviewActivity
import net.gini.android.health.sdk.exampleapp.util.SharedPreferencesUtil
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    useTestDocument: Boolean,
    testDocumentId: String,
    onOpenConfiguration: () -> Unit,
    onStartUpload: (List<Uri>) -> Unit,
    onOpenInvoicesM3: () -> Unit,
    onOpenInvoicesAppCompat: () -> Unit,
    onOpenOrders: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pages by viewModel.contentPages.collectAsState(initial = emptyList())
    val paymentRequest by viewModel.paymentRequest.collectAsState(initial = null)

    // Show toast when paymentRequest updates (replicates lifecycleScope collector)
    LaunchedEffect(paymentRequest) {
        paymentRequest?.let {
            Toast.makeText(
                context,
                "Paymentrequest: ${it.id} status is ${it.status.name}",
                Toast.LENGTH_SHORT
            ).show()
            SharedPreferencesUtil.saveStringToSharedPreferences(
                SharedPreferencesUtil.PAYMENTREQUEST_KEY,
                null,
                context
            )
        }
    }
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        if (saved) {
            viewModel.onPhotoSaved()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) {
            Toast.makeText(context, "No document received", Toast.LENGTH_LONG).show()
        } else {
            // Persist permission so the URIs remain readable after process death
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: SecurityException) {
                    // Some providers don't support persistable perms; safe to ignore.
                }
            }

            //  Add the imported URIs to the pages list and stay on this screen
            viewModel.addImportedUris(uris)
        }
    }


    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Scrollable content
        Column (
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.gini_logo),
                contentDescription = "Gini Logo",
                modifier = Modifier
                    .padding(top = 24.dp)
                    .sizeIn(maxHeight = 120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = stringResource(id = R.string.welcome_to_gini),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.example_of_health_sdk),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Pager with images (URIs from pages)
            val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .height(220.dp)
            ) { page ->
                val item = pages.getOrNull(page)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (item != null) {
                        // Show the captured/imported page by URI
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setImageURI(item.uri)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            "No pages yet",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            DotsIndicator(
                totalDots = max(1, pages.size),
                selectedIndex = min(pagerState.currentPage, max(0, (pages.size - 1)))
            )

            Spacer(Modifier.height(16.dp))

            // Buttons (order matches original screen)
            FilledTonalButton(
                onClick = {
                    if (useTestDocument) {
                        viewModel.setDocumentForReview(testDocumentId)
                        context.startActivity(ReviewActivity.getStartIntent(context))
                    } else {
                        importLauncher.launch(arrayOf("image/*", "application/pdf"))
                    }
                }
            ) { Text(stringResource(id = R.string.import_file)) }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = {
                    val uri = viewModel.getNextPageUri(context)
                    tempCaptureUri = uri
                    takePictureLauncher.launch(uri)
                }
            ) { Text(stringResource(id = R.string.take_photo)) }

            Spacer(Modifier.height(8.dp))

            // Enabled when at least one page exists (mirrors enabling after photo)
            val canUpload = pages.isNotEmpty()
            Button(
                onClick = {
                    onStartUpload(pages.map { it.uri })
                },
                enabled = canUpload
            ) { Text(stringResource(id = R.string.upload)) }

            Spacer(Modifier.height(8.dp))

            Button(onClick = onOpenInvoicesM3) {
                Text("Invoices list (Material 3 Theme)")
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = onOpenInvoicesAppCompat) {
                Text("Invoices list (AppCompat Theme)")
            }

            Spacer(Modifier.height(8.dp))

            Button(onClick = onOpenOrders) {
                Text(stringResource(id = R.string.button_orders_list))
            }

            Spacer(Modifier.height(12.dp))

            // SDK version + open configuration (fragment)
            val version = BuildConfig.VERSION_NAME
            Text(
                modifier = Modifier
                    .clickable { onOpenConfiguration() }
                    .padding(bottom = 12.dp),
                text = "${stringResource(id = R.string.gini_health_version)} $version",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }

    }
}
@Composable
fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    dotSpacing: Dp = 6.dp
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(totalDots) { index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = dotSpacing / 2)
                    .size(if (selected) dotSize * 1.25f else dotSize)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

