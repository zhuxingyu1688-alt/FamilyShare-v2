# 家庭共享 v2.0 改动对照

## ━━━ 新增文件 ━━━
| 文件 | 说明 |
|------|------|
| NetworkReceiver.kt | 监听网络变化，断网重连后自动恢复两个闹钟 |

## ━━━ 修改文件 ━━━

### Prefs.kt
| 改动 | 说明 |
|------|------|
| + KEY_SEND2_HOUR / KEY_SEND2_MINUTE | 定时②的小时/分钟存储键 |
| + KEY_NEXT_TRIGGER2 | 定时②的下次触发时间 |
| + getSend2Hour / getSend2Minute | 读定时②，默认 20:00 |
| + setSend2Time | 写定时② |
| + getSend2TimeText / getNextTriggerAt2 / getNextTriggerText2 | 定时②格式化显示 |
| + getScheduleSummary | 输出 "06:00 / 20:00" 格式的双时间摘要 |
| + getBothNextTriggers | 返回两个下次触发时间对 |

### AlarmScheduler.kt（重写）
| 改动 | 说明 |
|------|------|
| REQUEST_CODE_1=2001, REQUEST_CODE_2=2002 | 两个独立闹钟 ID |
| + EXTRA_ALARM_INDEX | 在 Intent 中标记是哪个闹钟触发 |
| schedule() → 安排两个时间 | 同时注册两枚 PendingIntent |
| + scheduleSpecific(alarmIndex) | 触发后只重排刚触发的那一个，另一个不动 |
| + rescheduleBothForTomorrow() | 今日暂停时两个都推到明天 |
| cancelAll() → 清除两个 | 同时取消两个 PendingIntent |

### AlarmReceiver.kt
| 改动 | 说明 |
|------|------|
| 从 intent 读取 EXTRA_ALARM_INDEX | 知道是哪个闹钟被触发 |
| 调用 scheduleSpecific() | 只重排触发的那一个 |
| 不再调用 rescheduleForTomorrow() | 旧逻辑只支持一个时间 |

### LocationService.kt（深度优化）
| 改动 | 说明 |
|------|------|
| OkHttpClient 单例化（companion lazy） | 避免每次发送创建新连接池 |
| + 发送重试（最多 2 次，间隔 3s） | 增加 postLocationWithRetry() |
| 移除弃用 onStatusChanged 回调 | 改用空实现避免现代 API 弃用警告 |
| 接收 EXTRA_ALARM_INDEX | 日志中显示 "定时1发送" / "定时2发送" |
| 文字消息标注 alarmIndex | 飞书推送中带 "定时①" 标签 |
| User-Agent → FamilyShare/4.0 | 版本号更新 |

### MainActivity.kt（重写）
| 改动 | 说明 |
|------|------|
| + npHour2/npMinute2/btnSaveTime2 | 定时②的 UI 绑定 |
| + tvNextTime2 | 状态面板显示定时②下次时间 |
| + setupSinglePicker() | 抽取通用 picker 设置方法 |
| + saveBothTimes() | 一键保存两个时间 |
| btnSaveTime → 只保存定时① | 独立保存，互不干扰 |
| btnSaveTime2 → 只保存定时② | 调用 AlarmScheduler.schedule() 重排两个 |
| btnToggle → 显示双时间摘要 | "开启每日 06:00 / 20:00 发送" |
| btnPauseToday → rescheduleBothForTomorrow | 两个都推到明天 |
| updateStatus → 显示两个下次时间 | tvNextTime + tvNextTime2 |
| showSelfCheckDialog → 显示双定时状态 | 含两个时间的计划 |

### activity_main.xml（重写）
| 改动 | 说明 |
|------|------|
| + 定时① Card（独立标题+选择器+保存按钮） | 第一组完整时间选择 |
| + 定时② Card（独立标题+选择器+保存按钮） | 第二组，默认 20:00 |
| + tvNextTime2 | 状态面板第二行 |
| 副标题改为 "双定时版" | 标识版本 |
| 底部说明更新 | |

### BootReceiver.kt
| 改动 | 说明 |
|------|------|
| 调用 schedule()（支持双定时） | 开机后同时恢复两个闹钟 |
| 日志输出两个下次时间 | 含 getBothNextTriggers() |

### AndroidManifest.xml
| 改动 | 说明 |
|------|------|
| + ACCESS_NETWORK_STATE 权限 | 网络监听需要 |
| + NetworkReceiver 注册 | intent-filter: CONNECTIVITY_CHANGE |
| 排列顺序调整 | 无逻辑影响 |

### app/build.gradle
| 改动 | 说明 |
|------|------|
| versionCode 1→2 | |
| versionName "1.0"→"2.0" | |
