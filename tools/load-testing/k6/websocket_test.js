# Solra Load Testing - WebSocket Real-time Scenario
# Simulates concurrent users in social spaces
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    scenarios: {
        websocket: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 200 },
                { duration: '1m',  target: 200 },
                { duration: '30s', target: 500 },
                { duration: '2m',  target: 500 },
                { duration: '1m',  target: 200 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        'ws_connecting': ['p(95)<1000'],
        'ws_msgs_received': ['count>0'],
    },
};

const TARGET = __ENV.TARGET_URL || 'wss://dev.solra.io';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export default function () {
    const userId = uuidv4();
    const url = `${TARGET}/ws/soc?token=${AUTH_TOKEN}&user_id=${userId}`;

    const res = ws.connect(url, {}, function (socket) {
        socket.on('open', function () {
            // Join space
            socket.send(JSON.stringify({
                type: 'join_space',
                space_id: 'test-space-001',
                user_id: userId,
                position: { x: Math.random() * 100, y: 0, z: Math.random() * 100 },
            }));

            // Send position updates periodically
            socket.setInterval(function () {
                socket.send(JSON.stringify({
                    type: 'position_update',
                    user_id: userId,
                    position: {
                        x: (Math.random() - 0.5) * 200,
                        y: 0,
                        z: (Math.random() - 0.5) * 200,
                    },
                    rotation: { yaw: Math.random() * 360 },
                    timestamp: Date.now(),
                }));
            }, 200); // 5Hz updates
        });

        socket.on('message', function (msg) {
            // Verify heartbeat / state sync
            const data = JSON.parse(msg);
            if (data.type === 'heartbeat') {
                socket.send(JSON.stringify({ type: 'heartbeat_ack' }));
            }
        });

        socket.on('close', function () {
            console.log(`VU ${__VU}: WebSocket closed`);
        });

        socket.on('error', function (e) {
            console.error(`VU ${__VU}: WebSocket error: ${e}`);
        });
    });

    check(res, { 'ws connected': (r) => r && r.status === 101 });

    sleep(Math.random() * 30 + 15); // 15-45s session
}
