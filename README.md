# 嘻嘻记事

一个完全本地的 Android 待办与规划应用。**无联网、无导入导出、不备份**，所有数据只留在你的手机上。

用「重要」和「紧急」两个标签把任务分到四个象限，优先级一眼可见：

```
重要且紧急  >  不重要但紧急  >  重要但不紧急  >  不重要且不紧急
```

---

## 功能

### 任务与四象限

- 每个任务两个可组合标签：**重要**、**紧急**，自动落入四象限
- 轻重缓急排序：四分组，组间按优先级，组内「我来做」优先 + 创建时间正序
- 每个分组可**独立折叠**，折叠状态写入 DataStore，跨进程保留；切到时间排序时忽略折叠，切回自动恢复
- 时间正序 / 时间倒序：平铺列表，无截止日期的任务永远排最后
- 「我来做」（assignee）三种规则：已勾选优先 / 仅视觉区分 / 分隔线区分（默认）
- 已完成两种显示：原位删除线 + 40% 透明（默认）/ 隐藏（顶栏眼睛图标找回）
- 完成、取消完成、删除都持久化到 Room

### 交互

- **Radial Menu**：点击 FAB 弹出 Canvas 自绘的整圆菜单，四个扇区「重急 / 轻急 / 重缓 / 轻缓」，拖动或点击选择象限后直接进编辑页并预置标签
- **Seek 搜索框**：44dp 圆形闭合 → 胶囊展开，实时过滤；搜索时切平铺列表、隐藏 FAB、其余顶栏按钮淡出缩小
- **Liq-Create 排序菜单**：46dp 圆形 → 212dp × 190dp 面板，480ms 同曲线展开，无 overshoot
- **Magnet-Select**：三颗小球切换「跟随系统 / 深色 / 浅色」，带磁铁式推挤与选中放大
- **底部 Dock**：4 标签，只缩放图标，选中项下方实心圆点；滚动时整体缩至 0.85，FAB 位置同步动画；点击当前标签回到顶部
- 撤销：完成后行下方显示「撤销」3 秒；非重要事项长按延迟删除 3 秒（应用被杀则任务保留、图片不删）
- 重要事项长按原地展开「删除此项？」Inline Confirm，点击外部收起
- 全程不使用默认 Dialog / Snackbar 作为信息载体，动效统一弹性曲线

### 图片

- 每个任务最多 **5 张**图片，来源为相册（Photo Picker）或拍照（FileProvider）
- 相册多选按剩余额度分发：剩余 ≥ 2 用 `PickMultipleVisualMedia(5)`，剩余 1 用单选；返回后只取前 N 张
- 无相机硬件或相机权限被拒时自动隐藏「拍照」
- 压缩：两阶段解码防 OOM，最大边 1920px、JPEG 85%，**不放大原图**
- EXIF：先复制到临时文件再读方向，旋转 90°/270° 后按互换后的宽高计算目标尺寸，压缩后把方向写回 `NORMAL`
- 顺序压缩（不并发），单张超时 30 秒并清理临时文件，失败时显示具体张数
- 全屏查看：双指缩放 1.0×–5.0×、放大后拖动平移、仅 1.0× 时单击关闭、底部页码、缺失图片显示「图片已丢失」且仍可滑动

### 提醒与通知

- AlarmManager + NotificationCompat，通知渠道「任务提醒」级别 High
- Android 12+ 无精确闹钟权限时自动降级为 `setAndAllowWhileIdle`
- `ReminderReceiver` 用 `goAsync()` + `Dispatchers.IO` 读数据库后发送通知
- 通知点击行为可在设置中切换：打开编辑页 / 打开主页 / 主页高亮任务
- 任务已删除时点击通知进主屏显示提示并清掉通知栏残留

### 统计与归档

- 统计页：四象限数量、本周完成（`java.time` + `WeekFields.ISO` 周一至周日）、总完成、逾期数，数字滚动 400ms
- 归档页：所有已完成任务按完成时间倒序，独立搜索框，结果按相关度排序（标题完全 > 标题包含 > 描述包含），支持取消完成与永久删除

### 其他

- 首次启动 3 页引导（重要与紧急 / Radial Menu / 提醒与主题），可跳过
- 设置页：已完成显示方式、assignee 排序、默认提醒分钟数、主题模式、动态取色（默认关闭，Android 12+）、通知点击行为、重看引导
- 锁定竖屏、跟随系统字体缩放、Edge-to-Edge、predictive back 友好
- 视觉：深色 #0B0B0D / 浅色 #F5F5F7 背景，卡片 #17171A / #FFFFFF，统一强调色 #4FD1B0（深色底）/ #0E9E82（浅色底），四象限色 重急 #F87171 / 急 #FBBF24 / 重缓 #60A5FA / 都不 #9CA3AF，中文 UI 字间距 0.3sp；圆角 token：条目卡片 16dp、主卡片 20dp、大卡片 24dp、弹层 28dp
- 文本样式：全部页面统一取用 `XixiTextStyles` 字阶 token（26 / 17 / 16 / 15 / 14 / 13 / 12 / 11sp），页面内不写 `fontSize` 魔法值，字重与颜色在调用处按语义覆盖

---

## 技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Kotlin 2.1.0 |
| UI | Jetpack Compose + Material 3（BOM 2025.01.00） |
| 图标 | Material Icons Extended |
| 架构 | 手动依赖注入（`AppContainer` + `LocalAppContainer`） |
| 数据库 | Room 2.7.0（KSP），版本 1 |
| 偏好 | DataStore Preferences |
| 导航 | Navigation Compose 2.8.5，单 NavHost |
| 图片 | Coil 2.7.0（仅本地文件）、ExifInterface 1.3.7 |
| 序列化 | kotlinx-serialization-json 1.7.3 |
| 异步 | Coroutines + StateFlow + Channel，无 LiveData |
| 构建 | Gradle 8.9、AGP 8.7.3、Java 17 |
| SDK | minSdk 26、targetSdk 35（Android 15） |

依赖全部为本地能力，**没有任何网络请求**，Coil 也只加载 `filesDir` 下的本地图片。

---

## 构建

需要 JDK 17 与 Android SDK（compileSdk 35）。

```bash
git clone <this-repo>
cd xixi-notes

# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

安装：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

也可以直接用 Android Studio 打开项目根目录后点击运行。

---

## GitHub Actions 下载

仓库内置工作流 `.github/workflows/build-apk.yml`：推送到 `main` 或手动触发后自动编译 Debug APK。

下载步骤：

1. 打开仓库的 **Actions** 页面
2. 选择最近一次 **Build Debug APK** 运行记录
3. 在页面底部 **Artifacts** 区域下载 `xixi-notes-debug`
4. 解压得到 `xixi-notes-debug.apk`

APK 使用 Debug 签名，可直接安装。

---

## 安装步骤

1. 把 `xixi-notes-debug.apk` 传到手机
2. 在系统设置中允许「安装未知应用」（仅首次需要）
3. 点击 APK 安装
4. 首次启动会展示 3 页引导；通知权限在首次设置提醒时间时申请

照片选择使用系统 Photo Picker，**不需要存储权限**。相机权限只在点击「拍照」时申请。

---

## 数据与隐私

- 数据库：`xixi_notes.db`（Room，仅本机）
- 图片：`filesDir/images/{taskId}/{uuid}.jpg`，临时文件在 `filesDir/images/temp/`
- 偏好：DataStore（`xixi_notes_prefs`）
- `android:allowBackup="false"`，且关闭云备份与设备迁移
- 无网络权限，无第三方统计，无导入导出

---

## 项目结构

```
xixi-notes/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── res/                        字符串、主题、启动画面、自适应图标、FileProvider 路径
│   └── java/com/xixi/notes/
│       ├── XixiNotesApp.kt          Application，创建 AppContainer
│       ├── MainActivity.kt          Splash / EdgeToEdge / 通知导航
│       ├── data/local/              TaskEntity、DAO、Database、TypeConverter
│       ├── data/preferences/        DataStore 偏好与折叠状态
│       ├── data/repository/         TaskRepository（唯一数据入口）
│       ├── di/                      AppContainer、NavEvent、RadialMenuState
│       ├── image/                   ImageManager（压缩/EXIF/临时文件）、ImageViewerBridge
│       ├── reminder/                调度器、Receiver、通知
│       └── ui/
│           ├── theme/               主题、色板、排版、形状
│           ├── main/                MainScaffold、NavHost、Dock、FAB、撤销
│           ├── board/               主屏列表、分组、排序、搜索
│           ├── detail/              编辑页与权限
│           ├── viewer/              全屏图片查看
│           ├── settings/            设置
│           ├── stats/               统计
│           ├── archive/             归档
│           ├── onboarding/          引导页
│           ├── components/          Seek / Radial Menu / Magnet-Select / Liq-Create / Dock / 任务行
│           └── util/                时间格式、排序比较器、动效常量
├── gradle/libs.versions.toml
├── build.gradle.kts / settings.gradle.kts / gradle.properties
└── .github/workflows/build-apk.yml
```
