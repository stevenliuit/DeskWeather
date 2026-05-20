#!/usr/bin/env python3
"""
WeatherClock Admin Server
提供 Web 界面 + REST API，用于通过浏览器配置 WeatherClock App 的所有功能。

访问地址：http://localhost:8080
"""

import json
import os
import subprocess
import threading
import time
from datetime import datetime
from flask import Flask, jsonify, request, send_from_directory, render_template_string

# =============================================================================
# 配置
# =============================================================================
PORT = 8080
APP_DIR = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(APP_DIR, "admin_config.json")

app = Flask(__name__, static_folder="static")

# =============================================================================
# 配置数据（内存 + 持久化）
# =============================================================================

DEFAULT_CONFIG = {
    "version": 1,
    "app": {
        "selected_city": "北京",
        "theme": "auto",
        "auto_refresh_minutes": 10,
    },
    "visual_style": "BALANCED",  # DATE_FOCUSED | CLOCK_FOCUSED | BALANCED
    "cities": {
        "pinned": [], "favorites": [], "recent": []
    }
}

def load_config():
    if os.path.exists(CONFIG_FILE):
        try:
            with open(CONFIG_FILE) as f:
                return json.load(f)
        except Exception:
            pass
    return dict(DEFAULT_CONFIG)

def save_config(cfg):
    with open(CONFIG_FILE, "w") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)

def get_config():
    if not hasattr(get_config, "_cfg"):
        get_config._cfg = load_config()
    return get_config._cfg

def put_config(cfg):
    get_config._cfg = cfg
    save_config(cfg)

# =============================================================================
# 同步到手机（ADB push + shell setprop 触发 reload）
# =============================================================================

def sync_to_phone():
    """
    将当前配置通过 ADB 推送到手机，
    利用 AppSettings 共享首选项机制实现实时同步。
    """
    cfg = get_config()

    # 写入临时文件
    phone_cfg_path = "/sdcard/weatherclock_admin.json"
    local_tmp = "/tmp/weatherclock_admin.json"

    with open(local_tmp, "w") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)

    try:
        # 推送到手机
        subprocess.run(["adb", "push", local_tmp, phone_cfg_path],
                       check=True, capture_output=True, timeout=10)

        # 通过 am broadcast 通知 App 重新读取配置
        # 方式：发送自定义广播，App 接收后重新加载 AppSettings
        subprocess.run([
            "adb", "shell", "am", "broadcast", "-a",
            "com.example.weatherclock.CONFIG_UPDATE",
            "-p", "com.desk.weather"
        ], check=False, capture_output=True, timeout=10)

        return True, "配置已同步到手机"
    except subprocess.TimeoutExpired:
        return False, "同步超时（手机未连接？）"
    except subprocess.CalledProcessError as e:
        return False, f"同步失败: {e.stderr.decode() if e.stderr else str(e)}"
    except Exception as e:
        return False, f"同步失败: {str(e)}"

def check_phone():
    """检查手机是否通过 ADB 连接"""
    try:
        r = subprocess.run(["adb", "get-state"], capture_output=True, timeout=5)
        return r.stdout.strip() == "device"
    except Exception:
        return False

# =============================================================================
# 模板：简洁的 Admin 页面
# =============================================================================

ADMIN_TEMPLATE = """
<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>WeatherClock Admin</title>
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
       background: #0d1117; color: #e6edf3; min-height: 100vh; }
.container { max-width: 900px; margin: 0 auto; padding: 24px; }
header { display: flex; justify-content: space-between; align-items: center;
         margin-bottom: 28px; padding-bottom: 16px;
         border-bottom: 1px solid #30363d; }
h1 { font-size: 24px; font-weight: 700; }
.status { font-size: 13px; color: #8b949e; display: flex; align-items: center; gap: 8px; }
.status .dot { width: 8px; height: 8px; border-radius: 50%; background: #f85149; }
.status.connected .dot { background: #3fb950; }
.card { background: #161b22; border: 1px solid #30363d; border-radius: 12px;
        padding: 20px; margin-bottom: 20px; }
.card h2 { font-size: 16px; font-weight: 600; margin-bottom: 16px;
            display: flex; align-items: center; gap: 8px; }
.form-group { margin-bottom: 14px; }
label { display: block; font-size: 13px; color: #8b949e; margin-bottom: 6px; }
input[type=text], select, input[type=number] {
  width: 100%; background: #0d1117; border: 1px solid #30363d;
  border-radius: 8px; padding: 8px 12px; color: #e6edf3; font-size: 14px;
  outline: none; transition: border 0.2s; }
input:focus, select:focus { border-color: #388bfd; }
input[type=number] { -moz-appearance: textfield; }
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button { -webkit-appearance: none; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; }
.toggle-item { display: flex; align-items: center; justify-content: space-between;
               padding: 10px 14px; background: #0d1117; border-radius: 8px;
               border: 1px solid #21262d; }
.toggle-item label { margin: 0; color: #e6edf3; font-size: 14px; }
.toggle { position: relative; width: 40px; height: 22px; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle .slider {
  position: absolute; cursor: pointer; inset: 0;
  background: #30363d; border-radius: 22px; transition: 0.3s;
}
.toggle .slider:before {
  content: ''; position: absolute; width: 16px; height: 16px;
  left: 3px; bottom: 3px; background: white; border-radius: 50%;
  transition: 0.3s;
}
.toggle input:checked + .slider { background: #388bfd; }
.toggle input:checked + .slider:before { transform: translateX(18px); }

.city-list { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }
.city-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px;
             background: #0d1117; border-radius: 8px; border: 1px solid #21262d; }
.city-item input { flex: 1; background: transparent; border: none; color: #e6edf3;
                   font-size: 14px; outline: none; }
.city-item .rm { color: #f85149; cursor: pointer; font-size: 16px; margin-left: auto; }

.btn { padding: 10px 20px; border-radius: 8px; font-size: 14px; font-weight: 500;
       cursor: pointer; border: none; transition: opacity 0.2s; }
.btn:hover { opacity: 0.85; }
.btn-primary { background: #238636; color: white; }
.btn-danger { background: #f85149; color: white; }
.btn-secondary { background: #30363d; color: #e6edf3; }
.btn-group { display: flex; gap: 10px; flex-wrap: wrap; }
.mt16 { margin-top: 16px; }
.msg { font-size: 13px; padding: 10px 14px; border-radius: 8px; margin-top: 12px; }
.msg.success { background: rgba(35,134,54,0.2); border: 1px solid #238636; color: #3fb950; }
.msg.error { background: rgba(248,81,73,0.2); border: 1px solid #f85149; color: #f85149; }
.last-sync { font-size: 12px; color: #8b949e; margin-top: 8px; }
.visual-style-item { padding: 12px; background: #0d1117; border-radius: 10px; border: 1px solid #30363d; cursor: pointer; transition: border 0.2s; }
.visual-style-item:hover { border-color: #388bfd; }
</style>
</head>
<body>
<div class="container">
  <header>
    <h1>🌤️ WeatherClock Admin</h1>
    <div class="status" id="status">
      <span class="dot"></span>
      <span id="statusText">检查连接...</span>
    </div>
  </header>

  <!-- 通用设置 -->
  <div class="card">
    <h2>⚙️ 通用设置</h2>
    <div class="grid">
      <div class="form-group">
        <label>默认城市</label>
        <input type="text" id="selectedCity" placeholder="例如：北京">
      </div>
      <div class="form-group">
        <label>主题</label>
        <select id="theme">
          <option value="auto">自动（跟随系统）</option>
          <option value="day">晴昼</option>
          <option value="dusk">黄昏</option>
          <option value="night">星空</option>
          <option value="forest">森林</option>
          <option value="ocean">海洋</option>
          <option value="digital_screen">电子屏</option>
          <option value="eink_screen">水墨屏</option>
        </select>
      </div>
      <div class="form-group">
        <label>视觉布局</label>
        <select id="visualStyle">
          <option value="BALANCED">均衡布局</option>
          <option value="DATE_FOCUSED">日期突出</option>
          <option value="CLOCK_FOCUSED">时钟突出</option>
        </select>
      </div>
      </div>
      <div class="form-group">
        <label>自动刷新间隔（分钟）</label>
        <input type="number" id="autoRefresh" min="1" max="60" value="10">
      </div>
    </div>
  </div>

  <!-- 视觉布局说明 -->
  <div class="card">
    <h2>📐 视觉布局</h2>
    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;">
      <div class="visual-style-item" id="vs_balanced" onclick="setVisualStyle('BALANCED')">
        <div style="text-align:center;">
          <div style="background:#1a2a3a;border-radius:8px;padding:12px;margin-bottom:8px;">
            <div style="height:16px;background:#4fc3f7;border-radius:4px;margin-bottom:4px;"></div>
            <div style="height:10px;background:#90caf9;border-radius:4px;"></div>
          </div>
          <strong>均衡布局</strong><br>
          <small style="color:#8b949e">时间=日期=天气</small>
        </div>
      </div>
      <div class="visual-style-item" id="vs_date" onclick="setVisualStyle('DATE_FOCUSED')">
        <div style="text-align:center;">
          <div style="background:#1a2a3a;border-radius:8px;padding:12px;margin-bottom:8px;">
            <div style="height:20px;background:#4fc3f7;border-radius:4px;margin-bottom:4px;"></div>
            <div style="height:8px;background:#90caf9;border-radius:4px;"></div>
          </div>
          <strong>日期突出</strong><br>
          <small style="color:#8b949e">日期大、时间中</small>
        </div>
      </div>
      <div class="visual-style-item" id="vs_clock" onclick="setVisualStyle('CLOCK_FOCUSED')">
        <div style="text-align:center;">
          <div style="background:#1a2a3a;border-radius:8px;padding:12px;margin-bottom:8px;">
            <div style="height:24px;background:#4fc3f7;border-radius:4px;margin-bottom:4px;"></div>
            <div style="height:6px;background:#90caf9;border-radius:4px;"></div>
          </div>
          <strong>时钟突出</strong><br>
          <small style="color:#8b949e">时钟大、日期小</small>
        </div>
      </div>
    </div>
    <div style="margin-top:12px;padding:10px 14px;background:#0d1117;border-radius:8px;border:1px solid #30363d;">
      <span style="color:#8b949e;font-size:13px;">💡 提示：布局自适应屏幕，内容不再分左右区域</span>
    </div>
  </div>

  <!-- 城市管理 -->
  <div class="card">
    <h2>🏙️ 城市管理</h2>
    <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px;">
      <div>
        <label>📌 置顶城市</label>
        <div class="city-list" id="pinnedList"></div>
        <div style="display:flex;gap:6px;margin-top:8px;">
          <input type="text" id="pinnedInput" placeholder="城市名" style="flex:1">
          <button class="btn btn-secondary" onclick="addCity('pinned')">+</button>
        </div>
      </div>
      <div>
        <label>⭐ 收藏城市</label>
        <div class="city-list" id="favoritesList"></div>
        <div style="display:flex;gap:6px;margin-top:8px;">
          <input type="text" id="favInput" placeholder="城市名" style="flex:1">
          <button class="btn btn-secondary" onclick="addCity('favorites')">+</button>
        </div>
      </div>
      <div>
        <label>🕐 最近访问</label>
        <div class="city-list" id="recentList"></div>
      </div>
    </div>
  </div>

  <!-- 操作 -->
  <div class="btn-group mt16">
    <button class="btn btn-primary" onclick="saveAll()">💾 保存并同步到手机</button>
    <button class="btn btn-secondary" onclick="loadFromPhone()">📥 从手机拉取配置</button>
    <button class="btn btn-danger" onclick="resetConfig()">🔄 重置为默认</button>
  </div>
  <div id="msgBox"></div>
  <div class="last-sync" id="lastSync"></div>
</div>

<script>
const API = '/api';

let cfg = {};

async function load() {
  // 加载本地配置
  try {
    const r = await fetch(API + '/config');
    if (r.ok) cfg = await r.json();
  } catch(e) { console.error(e); }

  // 检查手机连接
  checkPhone();

  // 填充表单
  document.getElementById('selectedCity').value = cfg.app?.selected_city || '';
  document.getElementById('theme').value = cfg.app?.theme || 'auto';
  document.getElementById('autoRefresh').value = cfg.app?.auto_refresh_minutes || 10;

  // 渲染视觉样式选中状态
  function renderVisualStyles() {
    const vs = cfg.visual_style || 'BALANCED';
    document.querySelectorAll('.visual-style-item').forEach(el => el.style.border = '1px solid #30363d');
    const el = document.getElementById('vs_' + vs.toLowerCase());
    if (el) el.style.border = '2px solid #388bfd';
    document.getElementById('visualStyle').value = vs;
  }

  function setVisualStyle(style) {
    cfg.visual_style = style;
    renderVisualStyles();
  }

  // 渲染城市列表
  renderCities('pinnedList', cfg.cities?.pinned || [], 'pinned');
  renderCities('favoritesList', cfg.cities?.favorites || [], 'favorites');
  renderCities('recentList', cfg.cities?.recent || [], 'recent');
  renderVisualStyles();

  const lastSync = cfg._last_sync;
  if (lastSync) {
    document.getElementById('lastSync').textContent = '上次同步：' + lastSync;
  }
}

function renderCities(listId, cities, type) {
  const container = document.getElementById(listId);
  if (cities.length === 0) {
    container.innerHTML = '<div style="font-size:13px;color:#8b949e;padding:8px">暂无</div>';
    return;
  }
  container.innerHTML = cities.map((c, i) => `
    <div class="city-item">
      <span>🌍</span><span style="flex:1">${c}</span>
      <span class="rm" onclick="removeCity('${type}', ${i})">✕</span>
    </div>
  `).join('');
}

function removeCity(type, idx) {
  cfg.cities[type].splice(idx, 1);
  renderCities(type + 'List', cfg.cities[type], type);
}

function addCity(type) {
  const inputId = type === 'pinned' ? 'pinnedInput' :
                  type === 'favorites' ? 'favInput' : null;
  if (!inputId) return;
  const val = document.getElementById(inputId).value.trim();
  if (!val) return;
  if (!cfg.cities) cfg.cities = {pinned:[], favorites:[], recent:[]};
  if (!cfg.cities[type]) cfg.cities[type] = [];
  if (!cfg.cities[type].includes(val)) cfg.cities[type].push(val);
  document.getElementById(inputId).value = '';
  renderCities(type + 'List', cfg.cities[type], type);
}

async function saveAll() {
  // 收集表单
  cfg.app = {
    selected_city: document.getElementById('selectedCity').value,
    theme: document.getElementById('theme').value,
    auto_refresh_minutes: parseInt(document.getElementById('autoRefresh').value) || 10
  };
  cfg.visual_style = document.getElementById('visualStyle').value;

  showMsg('正在同步到手机...', 'info');
  try {
    const r = await fetch(API + '/config', {
      method: 'PUT', headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(cfg)
    });
    const result = await r.json();
    if (r.ok) {
      cfg._last_sync = new Date().toLocaleString('zh-CN');
      document.getElementById('lastSync').textContent = '上次同步：' + cfg._last_sync;
      showMsg('✅ ' + result.message, 'success');
    } else {
      showMsg('❌ ' + result.error, 'error');
    }
  } catch(e) {
    showMsg('❌ 网络错误：' + e.message, 'error');
  }
}

async function loadFromPhone() {
  showMsg('正在从手机拉取...', 'info');
  try {
    const r = await fetch(API + '/config/phone', {method: 'POST'});
    const result = await r.json();
    if (r.ok) {
      cfg = result.config;
      load();
      showMsg('✅ 已从手机拉取最新配置', 'success');
    } else {
      showMsg('❌ ' + result.error, 'error');
    }
  } catch(e) {
    showMsg('❌ 网络错误：' + e.message, 'error');
  }
}

function resetConfig() {
  if (!confirm('确定重置为默认配置？')) return;
  cfg = JSON.parse(JSON.stringify({{ default_config_json|safe }}));
  saveAll();
}

function showMsg(text, type) {
  const box = document.getElementById('msgBox');
  box.innerHTML = '<div class="msg ' + type + '">' + text + '</div>';
  if (type === 'success') setTimeout(() => box.innerHTML = '', 4000);
}

async function checkPhone() {
  try {
    const r = await fetch(API + '/status');
    const d = await r.json();
    const el = document.getElementById('status');
    const txt = document.getElementById('statusText');
    if (d.phone_connected) {
      el.className = 'status connected';
      txt.textContent = '📱 已连接 ' + d.phone_model;
    } else {
      el.className = 'status';
      txt.textContent = '⚠️ 手机未连接';
    }
  } catch(e) {
    document.getElementById('statusText').textContent = '⚠️ API 离线';
  }
}

setInterval(checkPhone, 5000);
load();
</script>
</body>
</html>
"""

# =============================================================================
# API 路由
# =============================================================================

@app.route("/")
def index():
    default_json = json.dumps(DEFAULT_CONFIG, ensure_ascii=False)
    return render_template_string(ADMIN_TEMPLATE, default_config_json=default_json)

@app.route("/api/status")
def api_status():
    """手机连接状态"""
    connected = check_phone()
    model = ""
    if connected:
        try:
            r = subprocess.run(["adb", "shell", "getprop", "ro.product.model"],
                               capture_output=True, timeout=5)
            model = r.stdout.strip()
        except Exception:
            pass
    return jsonify({
        "phone_connected": connected,
        "phone_model": model,
        "server_time": datetime.now().isoformat()
    })

@app.route("/api/config", methods=["GET"])
def api_get_config():
    """获取当前配置"""
    return jsonify(get_config())

@app.route("/api/config", methods=["PUT"])
def api_put_config():
    """保存配置（本地 + 推送到手机）"""
    try:
        cfg = request.get_json()
        put_config(cfg)

        # 异步同步到手机
        threading.Thread(target=lambda: sync_to_phone(), daemon=True).start()
        return jsonify({"ok": True, "message": "配置已保存，正在同步到手机..."})
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)}), 400

@app.route("/api/config/phone", methods=["POST"])
def api_pull_from_phone():
    """从手机拉取配置"""
    # 通过 adb shell dumpsys 获取 AppSettings 共享首选项（需要 read权限）
    # 这里通过 read_shared_prefs.py 脚本来读取
    script = os.path.join(APP_DIR, "read_shared_prefs.py")
    if not os.path.exists(script):
        return jsonify({"ok": False, "error": "手机未连接或无法读取"}), 400
    try:
        r = subprocess.run(
            ["python3", script],
            capture_output=True, timeout=15, text=True
        )
        if r.returncode == 0 and r.stdout.strip():
            cfg = json.loads(r.stdout.strip())
            put_config(cfg)
            return jsonify({"ok": True, "config": cfg})
        else:
            return jsonify({"ok": False, "error": r.stderr or "读取失败"}), 400
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)}), 400

@app.route("/api/sync", methods=["POST"])
def api_sync():
    """手动触发同步到手机"""
    ok, msg = sync_to_phone()
    if ok:
        cfg = get_config()
        cfg["_last_sync"] = datetime.now().isoformat()
        put_config(cfg)
    return jsonify({"ok": ok, "message": msg})

@app.route("/api/logs", methods=["GET"])
def api_logs():
    """获取最近的 Git 提交记录"""
    try:
        r = subprocess.run(
            ["git", "log", "--oneline", "-10"],
            cwd=APP_DIR, capture_output=True, text=True, timeout=5
        )
        logs = r.stdout.strip().split("\n")
        return jsonify({"ok": True, "logs": logs})
    except Exception as e:
        return jsonify({"ok": False, "error": str(e)}), 400

# =============================================================================
# 静态资源 / 备用
# =============================================================================

@app.route("/static/<path:path>")
def static_files(path):
    return send_from_directory(app.static_folder, path)

# =============================================================================
# 启动
# =============================================================================

if __name__ == "__main__":
    print(f"🌤️  WeatherClock Admin Server")
    print(f"   访问地址: http://localhost:{PORT}")
    print(f"   API 端点: http://localhost:{PORT}/api")
    print(f"   配置文件: {CONFIG_FILE}")
    print()
    app.run(host="0.0.0.0", port=PORT, debug=False)