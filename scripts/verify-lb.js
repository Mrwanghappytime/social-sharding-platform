/**
 * WebSocket 负载均衡分布测试
 * 触发 N 次连接到 Gateway，分别保持 5 秒，统计每个 notification-service 实例收到的连接数
 */
const WebSocket = require('ws');

const GATEWAY = process.argv[2] || 'ws://localhost:8080/ws/notify';
const TOKEN = process.argv[3] || '';
const COUNT = parseInt(process.argv[4] || '10', 10);

if (!TOKEN) {
    console.error('Usage: node verify-lb.js <ws_url> <token> [count]');
    process.exit(1);
}

let connected = 0, closed = 0;
const results = [];

const start = Date.now();
console.log(`Opening ${COUNT} connections to ${GATEWAY}\n`);

for (let i = 0; i < COUNT; i++) {
    const url = `${GATEWAY}?token=${TOKEN}`;
    const ws = new WebSocket(url);

    ws.on('open', () => {
        connected++;
        console.log(`[${i}] connected (${connected}/${COUNT})`);
    });

    ws.on('error', (err) => {
        console.log(`[${i}] error: ${err.message}`);
    });

    ws.on('close', (code) => {
        closed++;
        if (closed === COUNT) {
            console.log(`\nAll ${COUNT} connections closed in ${Date.now() - start}ms`);
            process.exit(0);
        }
    });

    // 保持 5 秒后关闭
    setTimeout(() => ws.close(1000, 'done'), 5000);
}

// 兜底超时
setTimeout(() => {
    console.log('Timeout reached, exiting');
    process.exit(0);
}, 15000);
