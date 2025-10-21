# Tài Liệu Dự Án Dragun Cloud - Hệ Thống E-commerce

## 📋 Tổng Quan Dự Án

**Dragun Cloud** là một hệ thống e-commerce được xây dựng bằng Spring Boot, cung cấp các chức năng quản lý sản phẩm, đơn hàng, tài khoản người dùng và tích hợp với các dịch vụ bên ngoài như Pancake POS và MinIO.

### Thông Tin Cơ Bản
- **Tên dự án**: Dragun Cloud (Cake Project)
- **Phiên bản**: 1.0-SNAPSHOT
- **Framework chính**: Spring Boot 2.5.13
- **Java Version**: 11
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Storage**: MinIO (thay thế AWS S3)
- **Containerization**: Docker & Docker Compose

## 🏗️ Kiến Trúc Hệ Thống

### Cấu Trúc Thư Mục
```
dragun-cloud/
├── src/main/java/vn/co/cake/          # Mã nguồn Java
│   ├── Application.java               # Main class
│   ├── config/                        # Cấu hình
│   ├── controller/                    # REST Controllers
│   ├── entity/                        # JPA Entities
│   ├── service/                       # Business Logic
│   ├── repository/                    # Data Access Layer
│   ├── security/                      # Security Configuration
│   └── job/                          # Scheduled Jobs
├── src/main/resources/                # Resources
│   ├── static/                       # Static assets (CSS, JS, Images)
│   ├── templates/                    # Thymeleaf templates
│   └── application*.properties       # Configuration files
├── docker-compose.yml                # Docker services
├── Dockerfile                        # Application container
├── pom.xml                          # Maven dependencies
└── init/init.sql                    # Database initialization
```

## 🛠️ Công Nghệ Sử Dụng

### Backend Technologies
- **Spring Boot 2.5.13**: Framework chính
- **Spring Security**: Authentication & Authorization
- **Spring Data JPA**: ORM và database access
- **Spring Session**: Session management với Redis
- **Thymeleaf**: Template engine
- **QueryDSL**: Type-safe queries
- **Lombok**: Code generation
- **Jackson**: JSON processing

### Database & Cache
- **PostgreSQL 15**: Primary database
- **Redis 7**: Session storage và caching
- **HikariCP**: Connection pooling

### External Integrations
- **Pancake POS API**: Tích hợp hệ thống POS
- **MinIO**: Object storage (thay thế AWS S3)
- **SendGrid**: Email service
- **Facebook Business SDK**: Social media integration

### Frontend Technologies
- **Thymeleaf**: Server-side templating
- **Bootstrap 4**: CSS framework
- **jQuery**: JavaScript library
- **SCSS**: CSS preprocessing

## 🗄️ Cơ Sở Dữ Liệu

### Các Bảng Chính

#### 1. Quản Lý Người Dùng
- **account**: Thông tin tài khoản người dùng
- **users**: Bảng người dùng mở rộng

#### 2. Quản Lý Sản Phẩm
- **products**: Sản phẩm chính
- **thiyen_products**: Sản phẩm mở rộng (Thiyen)
- **products_extend**: Sản phẩm mở rộng
- **variations**: Biến thể sản phẩm (màu sắc, kích thước)
- **category**: Danh mục sản phẩm
- **product_gallery**: Thư viện ảnh sản phẩm
- **product_variants**: Biến thể sản phẩm

#### 3. Quản Lý Đơn Hàng
- **orders**: Đơn hàng
- **orders_extend**: Đơn hàng mở rộng
- **order_items**: Chi tiết đơn hàng
- **order_extend_items**: Chi tiết đơn hàng mở rộng
- **payments**: Thanh toán
- **vouchers**: Mã giảm giá

#### 4. Giỏ Hàng
- **carts**: Giỏ hàng
- **cart_items**: Sản phẩm trong giỏ hàng

#### 5. Địa Chỉ & Vị Trí
- **province**: Tỉnh/thành phố
- **districts**: Quận/huyện
- **wards**: Phường/xã
- **warehouse**: Kho hàng

#### 6. Tích Hợp Bên Ngoài
- **pancake_properties**: Cấu hình Pancake POS
- **webhook_history**: Lịch sử webhook
- **mail_history**: Lịch sử email

## 🎯 Chức Năng Chính

### 1. Quản Lý Sản Phẩm
- **CRUD sản phẩm**: Tạo, đọc, cập nhật, xóa sản phẩm
- **Quản lý biến thể**: Màu sắc, kích thước, giá
- **Thư viện ảnh**: Upload và quản lý nhiều ảnh sản phẩm
- **Danh mục**: Phân loại sản phẩm theo category
- **Tìm kiếm**: Tìm kiếm sản phẩm theo tên, danh mục
- **Tích hợp Pancake**: Đồng bộ sản phẩm với hệ thống POS

### 2. Quản Lý Đơn Hàng
- **Tạo đơn hàng**: Từ giỏ hàng hoặc mua trực tiếp
- **Theo dõi trạng thái**: Pending, Processing, Shipped, Delivered
- **Thanh toán**: Hỗ trợ nhiều phương thức thanh toán
- **Vận chuyển**: Tính phí ship, quản lý địa chỉ giao hàng
- **Mã giảm giá**: Áp dụng voucher, discount

### 3. Quản Lý Người Dùng
- **Đăng ký/Đăng nhập**: Authentication với Spring Security
- **Phân quyền**: Admin, Manager, User, Staff
- **Quản lý profile**: Thông tin cá nhân, địa chỉ
- **Quên mật khẩu**: Reset password qua email

### 4. Giỏ Hàng
- **Thêm/Xóa sản phẩm**: Quản lý giỏ hàng
- **Lưu trữ session**: Sử dụng Redis để lưu giỏ hàng
- **Tính toán**: Tự động tính tổng tiền, phí ship

### 5. Tích Hợp Bên Ngoài
- **Pancake POS**: Đồng bộ sản phẩm và đơn hàng
- **MinIO**: Lưu trữ file và hình ảnh
- **Email Service**: Gửi email thông báo
- **Facebook Integration**: Tích hợp mạng xã hội

## 🔧 Cấu Hình Triển Khai

### Docker Services

#### 1. Application (debase-app)
- **Port**: 8085
- **Environment Variables**:
  - `SPRING_REDIS_HOST`: debase-redis
  - `SPRING_DATASOURCE_URL`: jdbc:postgresql://debase-db:5432/debase
  - `MINIO_ENDPOINT`: http://debase-minio:9000

#### 2. Database (debase-db)
- **Image**: postgres:15
- **Port**: 5432
- **Database**: debase
- **User**: dragun
- **Password**: Picachu@123

#### 3. Redis (debase-redis)
- **Image**: redis:7
- **Port**: 6379
- **Configuration**: Custom redis.conf

#### 4. MinIO (debase-minio)
- **Image**: minio/minio:latest
- **Ports**: 9000 (API), 9001 (Console)
- **Credentials**: dragun / Picachu@123

### Profiles Configuration

#### Development Profile
- **Database**: Local PostgreSQL
- **Redis**: Local Redis
- **Logging**: Debug level
- **File Upload**: 100MB max

#### Production Profile
- **Database**: AWS RDS PostgreSQL
- **Redis**: AWS ElastiCache
- **Logging**: Info level
- **Security**: Enhanced

## 🚀 Hướng Dẫn Triển Khai

### 1. Yêu Cầu Hệ Thống
- Docker & Docker Compose
- Java 11+ (cho development)
- Maven 3.6+ (cho development)

### 2. Triển Khai với Docker
```bash
# Clone repository
git clone <repository-url>
cd dragun-cloud

# Build và chạy services
docker-compose up -d

# Kiểm tra logs
docker-compose logs -f debase-app
```

### 3. Truy Cập Services
- **Application**: http://localhost:8085
- **MinIO Console**: http://localhost:9001
- **Database**: localhost:5432
- **Redis**: localhost:6379

### 4. Cấu Hình Môi Trường
```bash
# Development
export SPRING_PROFILES_ACTIVE=development

# Production
export SPRING_PROFILES_ACTIVE=production
```

## 📱 Giao Diện Người Dùng

### Templates Chính
- **index.html**: Trang chủ với danh sách sản phẩm
- **product-detail.html**: Chi tiết sản phẩm
- **login.html**: Đăng nhập
- **register.html**: Đăng ký
- **cart-detail.html**: Giỏ hàng
- **order-status.html**: Trạng thái đơn hàng

### Responsive Design
- **Mobile-first**: Tối ưu cho thiết bị di động
- **Bootstrap 4**: Framework CSS
- **Custom SCSS**: Styling tùy chỉnh
- **jQuery**: JavaScript interactions

## 🔐 Bảo Mật

### Authentication & Authorization
- **Spring Security**: Framework bảo mật
- **JWT Tokens**: Stateless authentication
- **Role-based Access**: Phân quyền theo vai trò
- **Session Management**: Redis-based sessions

### Security Features
- **Password Encryption**: BCrypt
- **CSRF Protection**: Cross-site request forgery
- **CORS Configuration**: Cross-origin resource sharing
- **Input Validation**: Server-side validation

## 📊 Monitoring & Logging

### Logging Configuration
- **Logback**: Logging framework
- **Log Levels**: Configurable per package
- **File Logging**: /app/log/app.log
- **Console Logging**: Development environment

### Monitoring
- **Health Checks**: Spring Boot Actuator
- **Metrics**: Application metrics
- **Database Monitoring**: Connection pool status

## 🔄 CI/CD Pipeline

### Jenkins Configuration
- **Jenkinsfile**: Pipeline configuration
- **Build**: Maven build process
- **Test**: Automated testing
- **Deploy**: Docker deployment

### Build Process
```bash
# Maven build
mvn clean package

# Docker build
docker build -t dragun-cloud .

# Run tests
mvn test
```

## 📈 Performance Optimization

### Database Optimization
- **Connection Pooling**: HikariCP
- **Query Optimization**: QueryDSL
- **Indexing**: Database indexes
- **Caching**: Redis caching

### Application Optimization
- **Thread Pool**: Custom thread pool configuration
- **Static Resources**: Caching và compression
- **Session Management**: Redis-based sessions
- **File Upload**: Multipart configuration

## 🐛 Troubleshooting

### Common Issues
1. **Database Connection**: Kiểm tra PostgreSQL service
2. **Redis Connection**: Kiểm tra Redis service
3. **File Upload**: Kiểm tra MinIO service
4. **Memory Issues**: Tăng heap size

### Debug Commands
```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs [service-name]

# Restart service
docker-compose restart [service-name]

# Access container
docker-compose exec debase-app bash
```

## 📚 API Documentation

### REST Endpoints

#### Product Management
- `GET /api/thiyen/products/list` - Danh sách sản phẩm
- `GET /api/thiyen/products/{id}` - Chi tiết sản phẩm
- `POST /api/thiyen/products` - Tạo sản phẩm
- `PUT /api/thiyen/products/{id}` - Cập nhật sản phẩm
- `DELETE /api/thiyen/products/{id}` - Xóa sản phẩm

#### Order Management
- `GET /api/extend/orders` - Danh sách đơn hàng
- `POST /api/extend/orders` - Tạo đơn hàng
- `PUT /api/extend/orders/{id}/status` - Cập nhật trạng thái

#### External Integration
- `POST /api/v2/pancake/webhook` - Pancake webhook
- `POST /api/v2/pancake/sync-products` - Đồng bộ sản phẩm

## 🔮 Roadmap & Future Enhancements

### Planned Features
- **Mobile App**: React Native application
- **Advanced Analytics**: Business intelligence
- **Multi-language**: Internationalization
- **Advanced Search**: Elasticsearch integration
- **Microservices**: Service decomposition

### Technical Improvements
- **Spring Boot 3**: Upgrade framework
- **Java 17**: Latest LTS version
- **Kubernetes**: Container orchestration
- **Service Mesh**: Istio integration

## 📞 Support & Contact

### Development Team
- **Lead Developer**: [Dragun]
- **Backend Team**: [Dragun]
- **Frontend Team**: [Dragun]
- **DevOps Team**: [Dragun]

### Documentation
- **API Docs**: [Link to API documentation]
- **User Manual**: [Link to user guide]
- **Technical Specs**: [Link to technical specifications]

---

*Tài liệu này được cập nhật lần cuối: [Current Date]*
*Phiên bản: 1.0*
