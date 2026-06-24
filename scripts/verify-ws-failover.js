/**
 * WebSocket 故障转移验证脚本
 *
 * 用法:
 *   1. 先 npm install ws (在 scripts 目录)
 *   2. node verify-ws-failover.js [token] [count]
 *
 * 流程:
 *   1. 建立 N 个 WebSocket 长连接到 Gateway
 *   2. 打印每个连接的状态和服务端响应
 *   3. 监听断开事件并触发指数退避重连
 */

const WebSocket = require('ws');

const GATEWAY = 'ws://localhost:8080/ws/notify';
const TOKEN = process.argv[2] || '';
const COUNT = parseInt(process.argv[3] || '6', 10);

if (!TOKEN) {
    console.error('❌ Usage: node verify-ws-failover.js <JWT_TOKEN> [count]');
    process.exit(1);
}

const clients = [];

function createClient(id) {
    const url = `${GATEWAY}?token=${TOKEN}`;
    const ws = new WebSocket(url);
    let reconnectDelay = 1000;
    let connectedAt = null;

    const reconnect = () => {
        console.log(`[${id}] Reconnecting in ${reconnectDelay}ms...`);
        setTimeout(() => {
            const newWs = createClient(id);
            clients[id] = newWs;
        }, reconnectDelay);
        reconnectDelay = Math.min(reconnectDelay * 2, 30000);
    };

    ws.on('open', () => {
        connectedAt = Date.now();
        console.log(`[${id}] ✅ Connected (${new Date().toISOString().substring(11, 23)})`);
        reconnectDelay = 1000;
    });

    ws.on('message', (data) => {
        console.log(`[${id}] 📨 Received: ${data.toString()}`);
    });

    ws.on('close', (code, reason) => {
        const duration = connectedAt ? `${Date.now() - connectedAt}ms` : 'never connected';
        console.log(`[${id}] ❌ Closed code=${code} reason=${reason || 'unknown'} (held ${duration})`);
        reconnect();
    });

    ws.on('error', (err) => {
        console.log(`[${id}] ⚠️  Error: ${err.message}`);
    });

    return ws;
}

console.log(`Starting ${COUNT} WebSocket clients to ${GATEWAY}\n`);

for (let i = 0; i < COUNT; i++) {
    setTimeout(() => {
        clients[i] = createClient(i);
    }, i * 200);  // 每200ms启动一个
}

// 处理 Ctrl+C 优雅退出
process.on('SIGINT', () => {
    console.log('\nShutting down...');
    clients.forEach((ws, i) => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.close(1000, 'manual');
        }
    });
    process.exit(0);
});
