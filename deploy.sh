#!/bin/bash
# ============================================
# Telegram Admin Server 部署脚本
# 用于在 Docker 中部署后端服务并打包前端 App
# ============================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ADMIN_SERVER_DIR="${SCRIPT_DIR}/admin-server"
CLIENT_DIR="${SCRIPT_DIR}/TMessagesProj"
ADMIN_CONFIG_FILE="${CLIENT_DIR}/src/main/java/org/telegram/messenger/AdminConfig.java"

# 默认值
SERVER_IP=""
SERVER_PORT="8080"
ACTION=""

# ---- 帮助信息 ----
usage() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE} Telegram 部署脚本${NC}"
    echo -e "${BLUE}============================================${NC}"
    echo ""
    echo "用法: $0 --server-ip <IP> [选项]"
    echo ""
    echo "必需参数:"
    echo "  --server-ip <IP>       服务器公网 IP 地址"
    echo ""
    echo "可选参数:"
    echo "  --port <PORT>          服务端口 (默认: 8080)"
    echo "  --action <ACTION>      执行操作:"
    echo "                           all       - 部署后端 + 打包 App (默认)"
    echo "                           backend   - 仅部署后端服务"
    echo "                           app       - 仅打包 Android App"
    echo "                           stop      - 停止后端服务"
    echo "                           status    - 查看服务状态"
    echo "                           logs      - 查看服务日志"
    echo "  --env-file <FILE>      指定 .env 文件路径"
    echo "  -h, --help             显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --server-ip 192.168.1.100"
    echo "  $0 --server-ip 192.168.1.100 --port 8080 --action backend"
    echo "  $0 --server-ip 192.168.1.100 --action app"
    echo "  $0 --action stop"
    echo "  $0 --action logs"
    exit 0
}

# ---- 参数解析 ----
ENV_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --server-ip)
            SERVER_IP="$2"
            shift 2
            ;;
        --port)
            SERVER_PORT="$2"
            shift 2
            ;;
        --action)
            ACTION="$2"
            shift 2
            ;;
        --env-file)
            ENV_FILE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo -e "${RED}错误: 未知参数 $1${NC}"
            usage
            ;;
    esac
done

# ---- 工具函数 ----
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "\n${BLUE}========== $1 ==========${NC}\n"
}

# 跨平台 sed -i 封装（兼容 macOS 和 Linux）
portable_sed_i() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

# 生成随机密钥
generate_random_secret() {
    local length=${1:-64}
    if command -v openssl &> /dev/null; then
        openssl rand -base64 "$length" | tr -d '\n' | head -c "$length"
    else
        head -c "$length" /dev/urandom | base64 | tr -d '\n' | head -c "$length"
    fi
}

# ---- 检查依赖 ----
check_dependencies() {
    log_step "检查环境依赖"

    local missing=0

    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装。请先安装 Docker: https://docs.docker.com/get-docker/"
        missing=1
    else
        log_info "Docker: $(docker --version)"
    fi

    if ! command -v docker compose &> /dev/null && ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装。请先安装 Docker Compose。"
        missing=1
    else
        if command -v docker compose &> /dev/null; then
            log_info "Docker Compose: $(docker compose version 2>/dev/null)"
        else
            log_info "Docker Compose: $(docker-compose --version 2>/dev/null)"
        fi
    fi

    if [[ $missing -eq 1 ]]; then
        exit 1
    fi
}

# ---- Docker Compose 命令封装 ----
docker_compose_cmd() {
    if command -v docker compose &> /dev/null; then
        docker compose "$@"
    else
        docker-compose "$@"
    fi
}

# ---- 生成 .env 文件 ----
generate_env_file() {
    log_step "生成环境配置"

    local env_file="${SCRIPT_DIR}/.env"

    if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
        log_info "使用指定的 .env 文件: ${ENV_FILE}"
        cp "$ENV_FILE" "$env_file"
    fi

    # 如果 .env 文件不存在，从模板生成
    if [[ ! -f "$env_file" ]]; then
        if [[ -f "${SCRIPT_DIR}/.env.example" ]]; then
            cp "${SCRIPT_DIR}/.env.example" "$env_file"
            log_info "从 .env.example 模板生成 .env 文件"
        else
            log_info "创建新的 .env 文件"
            local random_jwt
            random_jwt=$(generate_random_secret 64)
            local random_db_pass
            random_db_pass=$(generate_random_secret 16)
            local random_admin_pass
            random_admin_pass=$(generate_random_secret 16)
            cat > "$env_file" << EOF
SERVER_IP=${SERVER_IP}
SERVER_PORT=${SERVER_PORT}
DB_USERNAME=sa
DB_PASSWORD=${random_db_pass}
JWT_SECRET=${random_jwt}
ADMIN_USERNAME=admin
ADMIN_PASSWORD=${random_admin_pass}
ADMIN_EMAIL=admin@telegram.local
CORS_ORIGINS=http://${SERVER_IP}:${SERVER_PORT}
SPRING_PROFILES_ACTIVE=production
EOF
            log_warn "已生成随机密码，请查看 .env 文件并记录管理员密码"
        fi
    fi

    # 更新 .env 文件中的 SERVER_IP 和 PORT
    if grep -q "^SERVER_IP=" "$env_file"; then
        portable_sed_i "s|^SERVER_IP=.*|SERVER_IP=${SERVER_IP}|" "$env_file"
    else
        echo "SERVER_IP=${SERVER_IP}" >> "$env_file"
    fi

    if grep -q "^SERVER_PORT=" "$env_file"; then
        portable_sed_i "s|^SERVER_PORT=.*|SERVER_PORT=${SERVER_PORT}|" "$env_file"
    else
        echo "SERVER_PORT=${SERVER_PORT}" >> "$env_file"
    fi

    # 确保 CORS_ORIGINS 包含服务器地址
    local server_url="http://${SERVER_IP}:${SERVER_PORT}"
    if grep -q "^CORS_ORIGINS=" "$env_file"; then
        local current_cors
        current_cors=$(grep "^CORS_ORIGINS=" "$env_file" | cut -d'=' -f2-)
        if [[ "$current_cors" != *"$server_url"* ]]; then
            if [[ -z "$current_cors" || "$current_cors" == "http://localhost:8080" ]]; then
                portable_sed_i "s|^CORS_ORIGINS=.*|CORS_ORIGINS=${server_url}|" "$env_file"
            else
                portable_sed_i "s|^CORS_ORIGINS=.*|CORS_ORIGINS=${current_cors},${server_url}|" "$env_file"
            fi
        fi
    else
        echo "CORS_ORIGINS=${server_url}" >> "$env_file"
    fi

    log_info "环境配置文件: ${env_file}"
    log_info "服务器地址: ${server_url}"
}

# ---- 构建后端 ----
build_backend() {
    log_step "构建后端服务 (Admin Server)"

    cd "$ADMIN_SERVER_DIR"

    # 检查 gradlew 是否存在且可执行
    if [[ ! -f "gradlew" ]]; then
        log_error "找不到 gradlew，请确认 admin-server 目录结构完整"
        exit 1
    fi
    chmod +x gradlew

    log_info "正在编译 admin-server.jar ..."
    ./gradlew bootJar --no-daemon -q

    if [[ -f "build/libs/admin-server.jar" ]]; then
        log_info "后端 JAR 构建成功: build/libs/admin-server.jar"
    else
        log_error "后端 JAR 构建失败"
        exit 1
    fi

    cd "$SCRIPT_DIR"
}

# ---- 部署后端 ----
deploy_backend() {
    log_step "Docker 部署后端服务"

    cd "$SCRIPT_DIR"

    # 加载 .env 文件
    if [[ -f ".env" ]]; then
        log_info "加载环境变量..."
    fi

    # 停止旧容器（如果存在）
    log_info "停止旧容器（如果存在）..."
    docker_compose_cmd down 2>/dev/null || true

    # 构建并启动容器
    log_info "构建 Docker 镜像..."
    docker_compose_cmd build

    log_info "启动容器..."
    docker_compose_cmd up -d

    # 等待服务启动
    log_info "等待服务启动..."
    local max_wait=60
    local waited=0
    while [[ $waited -lt $max_wait ]]; do
        if docker_compose_cmd ps 2>/dev/null | grep -q "healthy\|running"; then
            # 尝试访问健康检查端点
            if curl -sf "http://localhost:${SERVER_PORT}/api/client/configs" > /dev/null 2>&1; then
                log_info "后端服务启动成功！"
                break
            fi
        fi
        sleep 3
        waited=$((waited + 3))
        echo -n "."
    done
    echo ""

    if [[ $waited -ge $max_wait ]]; then
        log_warn "服务可能仍在启动中，请稍后检查"
        log_info "查看日志: $0 --action logs"
    fi

    # 显示服务信息
    echo ""
    log_info "=========================================="
    log_info "后端服务部署完成！"
    log_info "=========================================="
    log_info "管理面板: http://${SERVER_IP}:${SERVER_PORT}"
    log_info "API 地址: http://${SERVER_IP}:${SERVER_PORT}/api"
    log_info "客户端 API: http://${SERVER_IP}:${SERVER_PORT}/api/client"
    log_info "=========================================="
}

# ---- 更新 Android 客户端配置 ----
update_client_config() {
    log_step "更新 Android 客户端连接配置"

    if [[ ! -f "$ADMIN_CONFIG_FILE" ]]; then
        log_error "找不到 AdminConfig.java: ${ADMIN_CONFIG_FILE}"
        exit 1
    fi

    local server_url="http://${SERVER_IP}:${SERVER_PORT}"

    # 备份原文件
    cp "$ADMIN_CONFIG_FILE" "${ADMIN_CONFIG_FILE}.bak"

    # 更新 DEFAULT_SERVER_URL
    portable_sed_i "s|private static final String DEFAULT_SERVER_URL = \".*\";|private static final String DEFAULT_SERVER_URL = \"${server_url}\";|" "$ADMIN_CONFIG_FILE"

    # 验证修改
    if grep -q "DEFAULT_SERVER_URL = \"${server_url}\"" "$ADMIN_CONFIG_FILE"; then
        log_info "客户端配置已更新: DEFAULT_SERVER_URL = \"${server_url}\""
    else
        log_error "客户端配置更新失败，恢复备份"
        mv "${ADMIN_CONFIG_FILE}.bak" "$ADMIN_CONFIG_FILE"
        exit 1
    fi

    # 删除备份
    rm -f "${ADMIN_CONFIG_FILE}.bak"
}

# ---- 打包 Android App ----
build_app() {
    log_step "打包 Android App"

    cd "$SCRIPT_DIR"

    # 先更新客户端连接配置
    update_client_config

    log_info "使用 Docker 构建 Android APK..."
    log_info "（此过程可能需要 10-30 分钟，取决于网络和机器性能）"

    # 创建输出目录
    mkdir -p "${SCRIPT_DIR}/output/apk"
    mkdir -p "${SCRIPT_DIR}/output/bundle"

    # 使用根目录 Dockerfile 构建
    docker build -t telegram-app-builder -f "${SCRIPT_DIR}/Dockerfile" "${SCRIPT_DIR}"

    docker run --rm \
        -v "${SCRIPT_DIR}:/home/source" \
        telegram-app-builder

    # 检查输出
    if ls "${CLIENT_DIR}/build/outputs/apk/"*/*.apk 1> /dev/null 2>&1; then
        # 复制 APK 到 output 目录
        find "${CLIENT_DIR}/build/outputs/apk/" -name "*.apk" -exec cp {} "${SCRIPT_DIR}/output/apk/" \;
        log_info "APK 打包成功！"
        log_info "APK 输出目录: ${SCRIPT_DIR}/output/apk/"
        ls -la "${SCRIPT_DIR}/output/apk/"*.apk 2>/dev/null || true
    else
        log_warn "未找到 APK 文件，请检查构建日志"
    fi

    if ls "${CLIENT_DIR}/build/outputs/bundle/"*/*.aab 1> /dev/null 2>&1; then
        find "${CLIENT_DIR}/build/outputs/bundle/" -name "*.aab" -exec cp {} "${SCRIPT_DIR}/output/bundle/" \;
        log_info "Bundle 输出目录: ${SCRIPT_DIR}/output/bundle/"
    fi

    echo ""
    log_info "=========================================="
    log_info "App 打包完成！"
    log_info "=========================================="
    log_info "APK 文件: ${SCRIPT_DIR}/output/apk/"
    log_info "Bundle 文件: ${SCRIPT_DIR}/output/bundle/"
    log_info "客户端连接地址: http://${SERVER_IP}:${SERVER_PORT}"
    log_info "=========================================="
}

# ---- 停止服务 ----
stop_backend() {
    log_step "停止后端服务"
    cd "$SCRIPT_DIR"
    docker_compose_cmd down
    log_info "服务已停止"
}

# ---- 查看状态 ----
show_status() {
    log_step "服务状态"
    cd "$SCRIPT_DIR"
    docker_compose_cmd ps
}

# ---- 查看日志 ----
show_logs() {
    cd "$SCRIPT_DIR"
    docker_compose_cmd logs -f --tail=100
}

# ============================================
# 主逻辑
# ============================================

# 默认 action
if [[ -z "$ACTION" ]]; then
    ACTION="all"
fi

# 对于 stop/status/logs 操作不需要 SERVER_IP
case "$ACTION" in
    stop)
        stop_backend
        exit 0
        ;;
    status)
        show_status
        exit 0
        ;;
    logs)
        show_logs
        exit 0
        ;;
esac

# 其他操作需要 SERVER_IP
if [[ -z "$SERVER_IP" ]]; then
    log_error "请指定服务器 IP 地址: --server-ip <IP>"
    echo ""
    usage
fi

# 验证 IP 格式（支持 IP 和域名）
if [[ ! "$SERVER_IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] && [[ ! "$SERVER_IP" =~ ^[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?$ ]]; then
    log_error "无效的服务器地址: ${SERVER_IP}"
    exit 1
fi

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE} Telegram 部署${NC}"
echo -e "${BLUE}============================================${NC}"
echo -e " 服务器: ${SERVER_IP}:${SERVER_PORT}"
echo -e " 操作:   ${ACTION}"
echo -e "${BLUE}============================================${NC}"

check_dependencies

case "$ACTION" in
    all)
        generate_env_file
        build_backend
        deploy_backend
        build_app
        ;;
    backend)
        generate_env_file
        build_backend
        deploy_backend
        ;;
    app)
        generate_env_file
        update_client_config
        build_app
        ;;
    *)
        log_error "未知操作: ${ACTION}"
        usage
        ;;
esac

echo ""
log_info "部署完成！ 🎉"
