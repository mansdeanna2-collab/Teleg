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
GRADLE_PROPERTIES="${SCRIPT_DIR}/gradle.properties"

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
    echo "  --server-ip <IP>       服务器公网 IP 地址或域名"
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
    echo "                           restart   - 重启后端服务"
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
        openssl rand -base64 "$length" | tr -d '\n/+=\r' | head -c "$length"
    else
        head -c 256 /dev/urandom | base64 | tr -d '\n/+=\r' | head -c "$length"
    fi
}

# ---- 从 gradle.properties 读取版本信息 ----
read_gradle_property() {
    local key="$1"
    if [[ -f "$GRADLE_PROPERTIES" ]]; then
        grep "^${key}=" "$GRADLE_PROPERTIES" | cut -d'=' -f2- | tr -d '[:space:]'
    fi
}

get_app_version_info() {
    APP_VERSION_CODE=$(read_gradle_property "APP_VERSION_CODE")
    APP_VERSION_NAME=$(read_gradle_property "APP_VERSION_NAME")
    APP_PACKAGE=$(read_gradle_property "APP_PACKAGE")

    if [[ -z "$APP_VERSION_CODE" || -z "$APP_VERSION_NAME" ]]; then
        log_warn "无法从 gradle.properties 读取版本信息"
        APP_VERSION_CODE="unknown"
        APP_VERSION_NAME="unknown"
    fi

    log_info "应用版本: v${APP_VERSION_NAME} (code: ${APP_VERSION_CODE})"
    if [[ -n "$APP_PACKAGE" ]]; then
        log_info "应用包名: ${APP_PACKAGE}"
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

# ---- 部署后端（Docker 内构建 + 部署） ----
deploy_backend() {
    log_step "Docker 构建并部署后端服务"

    cd "$SCRIPT_DIR"

    # 验证 admin-server 目录存在
    if [[ ! -f "${ADMIN_SERVER_DIR}/build.gradle" ]]; then
        log_error "找不到 admin-server/build.gradle，请确认项目结构完整"
        exit 1
    fi

    # 加载 .env 文件
    if [[ -f ".env" ]]; then
        log_info "加载环境变量..."
    fi

    # 停止旧容器（如果存在）
    log_info "停止旧容器（如果存在）..."
    docker_compose_cmd down 2>/dev/null || true

    # 构建并启动容器（multi-stage Dockerfile 会自动编译 JAR）
    log_info "构建 Docker 镜像（含后端编译，首次可能需要几分钟）..."
    local build_ok=true
    docker_compose_cmd build || build_ok=false

    if [[ "$build_ok" != "true" ]]; then
        log_error "Docker 镜像构建失败"
        exit 1
    fi

    log_info "启动容器..."
    docker_compose_cmd up -d

    # 等待服务启动
    log_info "等待服务启动..."
    local max_wait=90
    local waited=0
    local started=false
    while [[ $waited -lt $max_wait ]]; do
        # 尝试访问健康检查端点
        if curl -sf "http://localhost:${SERVER_PORT}/api/client/configs" > /dev/null 2>&1; then
            started=true
            break
        fi
        sleep 3
        waited=$((waited + 3))
        echo -n "."
    done
    echo ""

    if [[ "$started" == "true" ]]; then
        log_info "后端服务启动成功！"
    else
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

    # 检查当前值是否已经正确
    if grep -q "DEFAULT_SERVER_URL = \"${server_url}\"" "$ADMIN_CONFIG_FILE"; then
        log_info "客户端配置已是最新: DEFAULT_SERVER_URL = \"${server_url}\""
        return 0
    fi

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

    # 读取版本信息
    get_app_version_info

    # 先更新客户端连接配置
    update_client_config

    log_info "使用 Docker 构建 Android APK..."
    log_info "版本: v${APP_VERSION_NAME} (code: ${APP_VERSION_CODE})"
    log_info "（首次构建需要下载 Android SDK + CMake，可能需要 30-60 分钟）"
    log_info "（后续构建会利用 Docker 缓存，速度更快）"

    # 创建输出目录
    mkdir -p "${SCRIPT_DIR}/output/apk"

    # 清理旧的构建输出（避免残留）
    rm -rf "${CLIENT_DIR}/build/outputs/apk" 2>/dev/null || true

    # 使用根目录 Dockerfile 构建 Android 编译环境镜像
    log_info "构建 Android 编译环境镜像（含 SDK, NDK, CMake）..."
    local build_ok=true
    docker build -t telegram-app-builder -f "${SCRIPT_DIR}/Dockerfile" "${SCRIPT_DIR}" || build_ok=false

    if [[ "$build_ok" != "true" ]]; then
        log_error "Android 编译环境镜像构建失败"
        exit 1
    fi

    # 运行编译容器（挂载源码目录）
    log_info "开始编译 APK（包含 JNI 原生代码编译）..."
    local run_ok=true
    docker run --rm \
        -v "${SCRIPT_DIR}:/home/source" \
        telegram-app-builder || run_ok=false

    if [[ "$run_ok" != "true" ]]; then
        log_error "APK 编译失败，请检查构建日志"
        log_info "提示: 可尝试增加 Docker 内存限制 (docker run --memory=8g)"
        exit 1
    fi

    # 收集输出文件（使用版本号命名）
    local apk_count=0
    while IFS= read -r apk_file; do
        # 从路径中提取变体名称（如 release/app.apk → telegram-v12.5.1-release.apk）
        local relative_path="${apk_file#${CLIENT_DIR}/build/outputs/apk/}"
        local variant_dir
        variant_dir=$(dirname "$relative_path")
        local variant_name
        variant_name=$(echo "$variant_dir" | tr '/' '-')
        local target_name="telegram-v${APP_VERSION_NAME}-${variant_name}.apk"

        cp "$apk_file" "${SCRIPT_DIR}/output/apk/${target_name}"
        log_info "  已收集: ${target_name}"
        apk_count=$((apk_count + 1))
    done < <(find "${CLIENT_DIR}/build/outputs/apk/" -name "*.apk" 2>/dev/null)

    if [[ $apk_count -gt 0 ]]; then
        log_info "APK 打包成功！共生成 ${apk_count} 个 APK"
        log_info "应用版本: v${APP_VERSION_NAME} (code: ${APP_VERSION_CODE})"
        log_info "APK 输出目录: ${SCRIPT_DIR}/output/apk/"
        ls -lh "${SCRIPT_DIR}/output/apk/"*.apk 2>/dev/null || true
    else
        log_warn "未找到 APK 文件，请检查构建日志"
    fi

    echo ""
    log_info "=========================================="
    log_info "App 打包完成！"
    log_info "=========================================="
    log_info "应用版本: v${APP_VERSION_NAME} (code: ${APP_VERSION_CODE})"
    log_info "APK 文件: ${SCRIPT_DIR}/output/apk/"
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

# ---- 重启服务 ----
restart_backend() {
    log_step "重启后端服务"
    cd "$SCRIPT_DIR"
    docker_compose_cmd restart
    log_info "服务已重启"
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

# 对于 stop/status/logs/restart 操作不需要 SERVER_IP
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
    restart)
        restart_backend
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
        deploy_backend
        build_app
        ;;
    backend)
        generate_env_file
        deploy_backend
        ;;
    app)
        generate_env_file
        build_app
        ;;
    *)
        log_error "未知操作: ${ACTION}"
        usage
        ;;
esac

echo ""
log_info "部署完成！ 🎉"
