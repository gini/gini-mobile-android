# bs_build_and_upload.sh

Builds the `example-app` and Espresso test suite APKs, uploads them to BrowserStack along with media files, and triggers a test run. By default runs the full `net.gini.android.bank.sdk.exampleapp.ui.testcases` package on both target devices; pass a fully qualified class name as `$1` to run a single class.

---

## Parameters

### Positional arguments

| Argument | Default | Description |
|---|---|---|
| `$1` — `TEST_CLASS` | _(none)_ | Fully qualified test class to run (e.g. `net.gini.android.bank.sdk.exampleapp.ui.testcases.CaptureScreenTests`). Omit to run the entire package. |

### Environment variables

| Variable | Default | Description |
|---|---|---|
| `BS_USER` | `<your_browserstack_user_name>` | BrowserStack username |
| `BS_KEY` | `<your_browserstack_access_key>` | BrowserStack access key |

### Fixed (not parameterised)

| Value | Description |
|---|---|
| `test_image.jpeg` | Camera injection file (`CaptureInjectionImage`) |
| `Testrechnung-RA-1.pdf` | PDF used in file picker upload tests (`UploadPDF`) |
| `sample.pdf` | PDF used in file picker upload tests (`SamplePDF`) |
| `Google Pixel 9-16.0` | Target BrowserStack device 1 |
| `Google Pixel 10 Pro-16.0` | Target BrowserStack device 2 |
| `net.gini.android.bank.sdk.exampleapp.ui.testcases` | Default test package (runs all test classes) |

All three media files are always uploaded. BrowserStack places them in the device's Downloads folder where the Espresso tests retrieve them via the system file picker.

---

## Output APKs

Both are written by Gradle into the `bank-sdk/example-app/build/outputs/apk/` folder:

- `devExampleApp/debug/example-app-dev-exampleApp-debug.apk` — app under test
- `androidTest/devExampleApp/debug/example-app-dev-exampleApp-debug-androidTest.apk` — Espresso test suite

---

## Usage examples

### Run with all defaults (full package, both devices)
```bash
cd bank-sdk/example-app/src/androidTest/scripts
./bs_build_and_upload.sh
```

### Run a single test class
```bash
./bs_build_and_upload.sh net.gini.android.bank.sdk.exampleapp.ui.testcases.DigitalInvoiceEditButtonTests
```

### Run with custom BrowserStack credentials
```bash
BS_USER="my_bs_username" BS_KEY="my_bs_key" ./bs_build_and_upload.sh
```

### Run with custom credentials and a single test class
```bash
BS_USER="my_bs_username" BS_KEY="my_bs_key" ./bs_build_and_upload.sh net.gini.android.bank.sdk.exampleapp.ui.testcases.CaptureScreenTests
```

### Export credentials for the session, then run multiple times
```bash
export BS_USER="my_bs_username"
export BS_KEY="my_bs_key"

./bs_build_and_upload.sh net.gini.android.bank.sdk.exampleapp.ui.testcases.DigitalInvoiceEditButtonTests
./bs_build_and_upload.sh net.gini.android.bank.sdk.exampleapp.ui.testcases.CaptureScreenTests
```

---

## What the script does

| Step | Description |
|---|---|
| 1 | Validates that all three media files exist before starting the build |
| 2 | Builds the app APK and test suite APK using Gradle (`assembleDevExampleAppDebug` + `assembleDevExampleAppDebugAndroidTest`) |
| 3 | Validates that both APK outputs exist |
| 4 | Uploads `test_image.jpeg` to BrowserStack as `CaptureInjectionImage` (camera injection) |
| 5 | Uploads `Testrechnung-RA-1.pdf` to BrowserStack as `UploadPDF` (file picker tests) |
| 6 | Uploads `sample.pdf` to BrowserStack as `SamplePDF` (file picker tests) |
| 7 | Uploads the app APK to the Espresso app endpoint |
| 8 | Uploads the test suite APK to the Espresso test-suite endpoint |
| 9 | Triggers the test run on both devices — scoped to `$1` (single class) if provided, otherwise the full package |

---

## Manual Debug Steps

Use these commands to upload and trigger tests individually from Terminal — useful when debugging a single step without re-running the full script.

Set your credentials first:
```bash
export BS_USER="your_browserstack_username"
export BS_KEY="your_browserstack_access_key"
```

---

### Step 1 — Upload camera injection image

```bash
curl -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/upload-media" \
  -F "file=@/path/to/test_image.jpeg" \
  -F "custom_id=CaptureInjectionImage"
```

Copy the `media_url` from the response (e.g. `media://abc123...`). You will need it in Step 6.

---

### Step 2 — Upload test PDF

```bash
curl -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/upload-media" \
  -F "file=@/path/to/Testrechnung-RA-1.pdf" \
  -F "custom_id=UploadPDF"
```

Copy the `media_url`. You will need it in Step 6.

---

### Step 3 — Upload sample PDF

```bash
curl -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/upload-media" \
  -F "file=@/path/to/sample.pdf" \
  -F "custom_id=SamplePDF"
```

Copy the `media_url`. You will need it in Step 6.

---

### Step 4 — Upload app APK

```bash
curl -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/app" \
  -F "file=@/path/to/example-app-dev-exampleApp-debug.apk"
```

Copy the `app_url` from the response (e.g. `bs://abc123...`). You will need it in Step 6.

> The script outputs the APK to `bank-sdk/example-app/build/outputs/apk/devExampleApp/debug/`.

---

### Step 5 — Upload test suite APK

```bash
curl -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/test-suite" \
  -F "file=@/path/to/example-app-dev-exampleApp-debug-androidTest.apk"
```

Copy the `test_suite_url` from the response (e.g. `bs://def456...`). You will need it in Step 6.

> The script outputs the test APK to `bank-sdk/example-app/build/outputs/apk/androidTest/devExampleApp/debug/`.

---

### Step 6 — Trigger test build

Replace `APP_URL`, `TEST_SUITE_URL`, `IMAGE_URL`, `PDF_URL`, and `SAMPLE_PDF_URL` with the values copied from the steps above.

**Run the full package:**
```bash
curl -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/build" \
  -H "Content-Type: application/json" \
  -d '{
    "project": "gini-mobile-android",
    "devices": ["Google Pixel 9-16.0", "Google Pixel 10 Pro-16.0"],
    "app": "APP_URL",
    "testSuite": "TEST_SUITE_URL",
    "package": ["net.gini.android.bank.sdk.exampleapp.ui.testcases"],
    "uploadMedia": ["IMAGE_URL", "PDF_URL", "SAMPLE_PDF_URL"]
  }'
```

**Run a single class:**
```bash
curl -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/build" \
  -H "Content-Type: application/json" \
  -d '{
    "project": "gini-mobile-android",
    "devices": ["Google Pixel 9-16.0", "Google Pixel 10 Pro-16.0"],
    "app": "APP_URL",
    "testSuite": "TEST_SUITE_URL",
    "class": ["net.gini.android.bank.sdk.exampleapp.ui.testcases.DigitalInvoiceEditButtonTests"],
    "uploadMedia": ["IMAGE_URL", "PDF_URL", "SAMPLE_PDF_URL"]
  }'
```

---

## `class` vs `package` explained

The `class` and `package` fields tell BrowserStack which tests to run from the uploaded test suite. Without either, BrowserStack runs every test in the APK.

| Field | Value | What runs |
|---|---|---|
| `package` | `net.gini.android.bank.sdk.exampleapp.ui.testcases` | All test classes in the package |
| `class` | `...testcases.DigitalInvoiceEditButtonTests` | Only `DigitalInvoiceEditButtonTests` |
| `class` | `...testcases.CaptureScreenTests` | Only `CaptureScreenTests` |

Multiple class names can be passed in the array to run a subset of classes.
