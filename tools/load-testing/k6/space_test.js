# Solra Load Testing - Space Service Scenario
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    scenarios: {
        browse_spaces: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 150 },
                { duration: '2m',  target: 150 },
                { duration: '1m',  target: 300 },
                { duration: '2m',  target: 300 },
                { duration: '30s', target: 0 },
            ],
        },
    },
    thresholds: {
        'http_req_duration{endpoint:recommend}': ['p(95)<300'],
        'http_req_duration{endpoint:space_detail}': ['p(95)<200'],
        'http_req_duration{endpoint:search}': ['p(95)<400'],
    },
};

const TARGET = __ENV.TARGET_URL || 'https://dev.solra.io';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || '';

export default function () {
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${AUTH_TOKEN}`,
        },
    };

    group('Browse Spaces', function () {
        // Get recommended spaces
        const recRes = http.get(`${TARGET}/api/v1/spaces/recommended?page=1&size=20`,
            Object.assign({}, params, { tags: { endpoint: 'recommend' } }));
        check(recRes, {
            'recommend 200': (r) => r.status === 200,
            'has items': (r) => r.json('items.length') > 0,
        });

        // Get space detail
        if (recRes.status === 200) {
            const items = recRes.json('items');
            const spaceId = items[Math.floor(Math.random() * items.length)].id;
            const detailRes = http.get(`${TARGET}/api/v1/spaces/${spaceId}`,
                Object.assign({}, params, { tags: { endpoint: 'space_detail' } }));
            check(detailRes, { 'detail 200': (r) => r.status === 200 });
        }

        // Search
        const searchRes = http.get(`${TARGET}/api/v1/spaces/search?q=forest&page=1&size=10`,
            Object.assign({}, params, { tags: { endpoint: 'search' } }));
        check(searchRes, { 'search 200': (r) => r.status === 200 });
    });

    sleep(Math.random() * 3 + 1); // 1-4s think time
}
