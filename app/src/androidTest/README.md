# Android Instrumented Tests - Hệ thống Giám sát Trạng thái Người Lái

## 📋 Tổng quan

Thư mục này chứa các **Instrumented Tests** (Android Tests) được viết bằng Kotlin sử dụng **JUnit 4** và **Espresso** để kiểm thử chức năng UI và tích hợp của ứng dụng DMS (Driver Monitoring System).

## 📁 Cấu trúc Tests

```
app/src/androidTest/java/com/example/dmsapplication/
├── tests/
│   ├── TC1_LoginTest.kt                 [✅ 8 test methods]
│   ├── TC2_RegisterTest.kt              [✅ 7 test methods]
│   ├── TC3_LogoutTest.kt                [✅ 5 test methods]
│   ├── TC5_MonitoringSettingsTest.kt    [✅ 10 test methods]
│   └── TC8_ViolationDetectionTest.kt    [✅ 12 test methods]
├── README.md                             [Documentation]
└── BUILD_GRADLE_ADDITIONS.md            [Setup guide]
```

**Tổng cộng:** 5 test classes, 42 test methods tập trung trong 1 package `com.example.dmsapplication.tests`

---

## 🚀 Cách chạy tests

### **1. Chạy tất cả tests trong module:**
```bash
./gradlew connectedAndroidTest
```

### **2. Chạy một test class cụ thể:**
```bash
# TC1 - Login Tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.dmsapplication.tests.TC1_LoginTest

# TC2 - Register Tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.dmsapplication.tests.TC2_RegisterTest

# TC3 - Logout Tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.dmsapplication.tests.TC3_LogoutTest

# TC5 - Monitoring Settings Tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.dmsapplication.tests.TC5_MonitoringSettingsTest

# TC8 - Violation Detection Tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.dmsapplication.tests.TC8_ViolationDetectionTest
```

### **3. Chạy một test method cụ thể:**
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.dmsapplication.tests.TC1_LoginTest#testLoginWithValidCredentials_shouldNavigateToHome
```

### **4. Chạy từ Android Studio:**
1. Mở file test class (ví dụ: `TC1_LoginTest.kt`)
2. Click vào icon ▶️ màu xanh bên cạnh class name hoặc method name
3. Chọn **Run 'TC1_LoginTest'** hoặc **Run 'testMethodName()'**

---

## 📊 Test Cases Chi tiết

### **TC1: Chức năng Đăng nhập** ✅

**File:** `tests/TC1_LoginTest.kt`
**Test methods:** 8
**Mục tiêu:** Xác thực quyền truy cập của tài xế vào ứng dụng

**Các test:**
1. ✓ Đăng nhập thành công với thông tin hợp lệ
2. ✓ Hiển thị lỗi khi để trống trường
3. ✓ Hiển thị lỗi khi email không đúng định dạng
4. ✓ Hiển thị lỗi khi mật khẩu sai
5. ✓ Nút đăng nhập hiển thị và clickable
6. ✓ Chuyển sang màn hình đăng ký
7. ✓ Chuyển sang màn hình quên mật khẩu
8. ✓ Quay lại từ màn hình quên mật khẩu

**Chạy:**
```bash
./run-tests.sh -t TC1
```

---

### **TC2: Chức năng Đăng ký** ✅

**File:** `tests/TC2_RegisterTest.kt`
**Test methods:** 7
**Mục tiêu:** Cho phép tài xế mới tạo tài khoản hệ thống

**Các test:**
1. ✓ Đăng ký thành công với dữ liệu hợp lệ
2. ✓ Lỗi khi để trống trường bắt buộc
3. ✓ Lỗi khi email không đúng định dạng
4. ✓ Lỗi khi mật khẩu dưới 6 ký tự
5. ✓ DatePicker mở khi click vào trường ngày sinh
6. ✓ Chuyển về màn hình đăng nhập
7. ✓ Tất cả view bắt buộc hiển thị

**Chạy:**
```bash
./run-tests.sh -t TC2
```

---

### **TC3: Chức năng Đăng xuất** ✅

**File:** `tests/TC3_LogoutTest.kt`
**Test methods:** 5
**Mục tiêu:** Hủy phiên làm việc hiện tại một cách an toàn

**Các test:**
1. ✓ Nút đăng xuất hiển thị trong Settings
2. ✓ Đăng xuất thành công
3. ✓ Settings Fragment truy cập được
4. ✓ Menu items hiển thị và clickable
5. ✓ Bottom navigation highlighting

**Chạy:**
```bash
./run-tests.sh -t TC3
```

---

### **TC5: Chức năng Thiết lập chế độ giám sát** ✅

**File:** `tests/TC5_MonitoringSettingsTest.kt`
**Test methods:** 10
**Mục tiêu:** Cấu hình các điều kiện kích hoạt luồng giám sát

**Các test:**
1. ✓ Tất cả switch thiết lập hiển thị
2. ✓ Bật/tắt giám sát GPS
3. ✓ Bật/tắt camera preview
4. ✓ Bật/tắt chế độ kính râm
5. ✓ Bật/tắt giám sát ngáp
6. ✓ Bật/tắt lưới FaceMesh
7. ✓ Trạng thái switch được lưu
8. ✓ Nút recalibrate hiển thị
9. ✓ Label text chính xác
10. ✓ Status và counters hiển thị

**Chạy:**
```bash
./run-tests.sh -t TC5
```

---

### **TC8: Chức năng Theo dõi hành trình và vi phạm** ✅

**File:** `tests/TC8_ViolationDetectionTest.kt`
**Test methods:** 12
**Mục tiêu:** Đếm và ghi nhận lỗi mất tập trung của tài xế theo thời gian thực

**Các test:**
1. ✓ Counter ban đầu = 0
2. ✓ Phát hiện buồn ngủ
3. ✓ Phát hiện quay đầu
4. ✓ Phát hiện ngáp
5. ✓ Ghi nhận vị trí GPS
6. ✓ Trạng thái thay đổi
7. ✓ Nhiều vi phạm tích lũy
8. ✓ Counter reset
9. ✓ Camera components hiển thị
10. ✓ Speed indicator hiển thị
11. ✓ GPS monitoring affect detection
12. ✓ Hello text hiển thị

**Chạy:**
```bash
./run-tests.sh -t TC8
```

---

## 📦 Dependencies cần thiết

Đảm bảo `build.gradle.kts` (Module: app) đã có đầy đủ dependencies (đã được thêm sẵn):

```kotlin
dependencies {
    // AndroidX Test - Core
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // AndroidX Test - JUnit
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    // Espresso - UI Testing
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")

    // Mockito - Mocking Framework
    androidTestImplementation("org.mockito:mockito-android:5.3.1")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    // Kotlin Coroutines Test
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // UI Automator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

    // Truth
    androidTestImplementation("com.google.truth:truth:1.1.5")
}
```

✅ **Build.gradle.kts đã CHUẨN!** Tất cả dependencies cần thiết đã được thêm.

---

## ⚙️ Cấu hình `build.gradle.kts`

✅ **Đã được cấu hình sẵn:**

```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}
```

---

## 📈 Test Reports

### **HTML Report**
Sau khi chạy tests, mở report:
```
app/build/reports/androidTests/connected/index.html
```

### **Coverage Report**
Chạy với coverage:
```bash
./gradlew createDebugCoverageReport
```

Xem report tại:
```
app/build/reports/coverage/androidTest/debug/index.html
```

---

## 🔧 Advanced Configuration

### **1. Chạy trên thiết bị cụ thể**

```bash
# List devices
adb devices

# Run on specific device
ANDROID_SERIAL=emulator-5554 ./gradlew connectedAndroidTest

# Hoặc với script
./run-tests.sh -t TC1 -d emulator-5554
```

### **2. Parallel execution (nhiều devices)**

Thêm vào `gradle.properties`:
```properties
android.testInstrumentationRunnerArguments.numShards=2
android.testInstrumentationRunnerArguments.shardIndex=0
```

---

## 🐛 Troubleshooting

### **Problem: Tests không chạy**

**Giải pháp:**
```bash
# Clean và rebuild
./gradlew clean
./gradlew assembleDebug assembleDebugAndroidTest

# Uninstall app từ device
adb uninstall com.example.dmsapplication
adb uninstall com.example.dmsapplication.test

# Chạy lại
./gradlew connectedAndroidTest
```

### **Problem: Emulator chậm**

**Giải pháp:**
- Sử dụng x86_64 emulator thay vì ARM
- Enable hardware acceleration (HAXM/KVM)
- Animations đã tắt trong testOptions

---

## 🎯 Best Practices

### ✅ **DO:**
- Viết tests độc lập, không phụ thuộc nhau
- Sử dụng `@Before` và `@After` để setup/cleanup
- Đặt tên test rõ ràng: `testFeature_condition_expectedResult`
- Kiểm tra cả positive và negative cases

### ❌ **DON'T:**
- Gọi API thực tế trong tests
- Hardcode delays (`Thread.sleep()`)
- Phụ thuộc vào thứ tự chạy tests
- Bỏ qua cleanup trong `@After`

---

## ✅ Tóm tắt

✅ **Build.gradle.kts:** CHUẨN - Đã có đầy đủ dependencies
✅ **Tests:** Không có lỗi syntax
✅ **Cấu trúc:** Đã tổ chức lại vào package `tests` tập trung
✅ **Package:** `com.example.dmsapplication.tests`
✅ **Total:** 5 classes, 42 methods

---

## 📚 Resources

- [Espresso Testing](https://developer.android.com/training/testing/espresso)
- [AndroidX Test](https://developer.android.com/training/testing/set-up-project)
- [Mockito Kotlin](https://github.com/mockito/mockito-kotlin)

---

**Happy Testing! 🧪✅**
