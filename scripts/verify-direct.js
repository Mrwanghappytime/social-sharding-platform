/**
 * 单连接测试 - 绕过 Gateway，直连 notification-service 端口 8085
 * 用于对比 Gateway 路由 vs 直连 的差异
 */
const WebSocket = require('ws');

const TARGET = process.argv[2] || 'ws://localhost:8085/ws/notify';
const TOKEN = process.argv[3] || '';

if (!TOKEN) {
    console.error('Usage: node verify-direct.js <ws_url> <token>');
    process.exit(1);
}

const url = `${TARGET}?token=${TOKEN}`;
console.log('Connecting to:', url.replace(/token=[^&]+/, 'token=***'));

const ws = new WebSocket(url);

ws.on('open', () => {
    console.log(`✅ Connected at ${new Date().toISOString()}`);
});

ws.on('message', (data) => {
    console.log(`📨 Message: ${data.toString()}`);
});

ws.on('close', (code, reason) => {
    console.log(`❌ Closed code=${code} reason=${reason || 'unknown'}`);
    process.exit(0);
});

ws.on('error', (err) => {
    console.log(`⚠️  Error: ${err.message}`);
});

setTimeout(() => {
    console.log('Held connection for 10s, closing manually');
    ws.close(1000, 'manual');
}, 10000);
