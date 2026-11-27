# Puppy's Home

Website giới thiệu dịch vụ chăm sóc thú cưng và quản lý nội dung, dịch vụ, bảng giá.

## Công nghệ

- Frontend: React 19, TypeScript, Vite, Nginx
- Backend: Java 25, Spring Boot 4, Flyway, Spring Security
- Database: MySQL 8.0
- Containerization: Docker & Docker Compose
- Version control: Git

## Cấu trúc dự án

- `frontend/`: Giao diện người dùng (React SPA, Nginx reverse proxy, Dockerfile)
- `backend/`: REST API, Flyway migration và nghiệp vụ (Spring Boot, Dockerfile)
- `md-AI/`: Tài liệu kiến trúc, quy tắc, kiểm thử và nhật ký phát triển

---

## Khởi chạy với Docker Compose

### 1. Yêu cầu tiên quyết
- Docker Engine / Docker Desktop (phiên bản hỗ trợ Compose v2 / `docker-compose`).
- Đảm bảo Docker daemon đang hoạt động.

### 2. Cấu hình môi trường
Sao chép file cấu hình mẫu `.env.example` thành `.env`:
```bash
cp .env.example .env
```

Các biến môi trường mặc định:
- `FRONTEND_PORT=3000`: Port truy cập giao diện web trên máy host.
- `BACKEND_PORT=8080`: Port truy cập API backend trên máy host.
- `DB_PORT=3307`: Port truy cập MySQL trên máy host (mặc định chọn 3307 để tránh xung đột nếu máy host đã chạy MySQL local trên 3306).
- `DB_NAME=puppys_home`
- `DB_USERNAME=puppy_app`
- `DB_PASSWORD=puppy_secret_password`
- `DB_ROOT_PASSWORD=root_secret_password`

### 3. Port Mapping

| Service | Container Port | Host Port (Mặc định) | Mô tả |
| --- | --- | --- | --- |
| **frontend** | 80 | 3000 (`http://localhost:3000`) | Giao diện React SPA qua Nginx; reverse proxy `/api` tới backend |
| **backend** | 8080 | 8080 (`http://localhost:8080`) | Spring Boot REST API |
| **db** | 3306 | 3307 (`localhost:3307`) | MySQL Database 8.0 |

### 4. Các lệnh quản lý Docker

- **Build và khởi động toàn bộ hệ thống (chạy ngầm):**
  ```bash
  docker-compose up -d --build
  ```
  *(hoặc `docker compose up -d --build`)*

- **Kiểm tra trạng thái các container và Health Check:**
  ```bash
  docker-compose ps
  ```
  *Hệ thống thiết lập dependency ordering chặt chẽ: `db` healthy -> `backend` chạy Flyway migration & healthy -> `frontend` khởi động.*

- **Xem log hệ thống theo thời gian thực:**
  ```bash
  docker-compose logs -f
  # Hoặc xem log từng service:
  docker-compose logs -f backend
  docker-compose logs -f db
  docker-compose logs -f frontend
  ```

- **Dừng hệ thống (Dữ liệu database được giữ nguyên trong volume):**
  ```bash
  docker-compose down
  ```

- **Reset toàn bộ dữ liệu database:**
  > [!WARNING]
  > **CẢNH BÁO MẤT DỮ LIỆU**: Lệnh dưới đây sẽ xóa sạch volume `puppyshome_db_data`. Toàn bộ dữ liệu MySQL sẽ bị xóa vĩnh viễn và database sẽ được tái tạo lại từ đầu ở lần khởi động kế tiếp.
  ```bash
  docker-compose down -v
  ```

### 5. Kiểm tra sau khi khởi chạy
- **Frontend SPA**: Mở trình duyệt tại [http://localhost:3000](http://localhost:3000)
- **Backend Health Check**: [http://localhost:8080/api/health](http://localhost:8080/api/health) hoặc qua proxy [http://localhost:3000/api/health](http://localhost:3000/api/health) (trả về `{"status":"UP","application":"puppys-home"}`)
- **Services API**: [http://localhost:3000/api/services](http://localhost:3000/api/services)
