# Bubu Dudu Pet (nuoithuoffline) 🐼🐻

Ứng dụng **nuôi thú ảo 2D Bubu & Dudu** theo phong cách Tamagotchi cổ điển kết hợp **Widget sống ngoài màn hình chính**, chạy **100% offline**, tối giản, không quảng cáo và cực tiết kiệm pin.

## Tính năng nổi bật

- **Cặp gấu 2D Bubu & Dudu đồng hành theo thời gian thực**:
  - Sáng (`06:00 - 08:30`): Tự động đánh răng cùng nhau
  - Trưa & Tối (`11:00 - 13:30`, `18:00 - 19:30`): Ngồi ăn cơm cùng nhau
  - Đêm (`22:00 - 05:59`): Đắp chăn ngủ ngon
  - Các khung giờ khác: Thư giãn, vui chơi hoặc nhắc khi đói / phòng bừa bộn
- **Chăm sóc & Dọn phòng tương tác**:
  - 4 chỉ số thời gian thực: **No bụng**, **Sạch sẽ**, **Vui vẻ**, **Năng lượng**
  - Vết bụi/vụn bánh xuất hiện trong phòng theo thời gian — chạm trực tiếp để quét sạch hoặc bấm **Dọn phòng**
  - Xoa đầu gấu để thả tim tăng độ thân thiết & lên cấp
- **Tủ đồ Mũ & Kính 2D**:
  - Tự chọn & phối mũ cói, mũ len, vương miện, kẹp hoa, kính râm tròn... hiển thị đồng bộ trên cả màn hình chính trong app và Widget ngoài Home
- **Mini-game kiếm Xu offline**:
  - *Hứng Đồ Ăn & Xu* (30 giây) và *Oẳn Tù Tì cùng Dudu* để kiếm Xu mở khóa thêm phụ kiện 2D
- **Home Screen Widget sống**:
  - Hiển thị trạng thái 2D kèm mũ/kính đang đeo, thanh No bụng / Vui vẻ và nút bấm nhanh **Cho ăn**, **Dọn phòng**, **Xoa đầu** ngay trên màn hình chính Android

## Công nghệ & Build

- Kotlin + Jetpack Compose + Material 3 + WorkManager + Android `AppWidgetProvider`
- `minSdk = 26` (Android 8.0+), `targetSdk = 34`, bật R8 minify + shrinkResources
- Không xin quyền Internet — toàn bộ dữ liệu lưu cục bộ trên máy
- File cài đặt sẵn: [BubuDuduPet.apk](./BubuDuduPet.apk) (`v1.0.0` • ~1.27 MB)

```powershell
$env:JAVA_HOME="D:\Android\jdk-21"
.\gradlew.bat assembleRelease --no-daemon
```
