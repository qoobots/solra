# Solra Load Testing Suite
# K6-based load testing scenarios for all microservices
# Usage: k6 run scripts/auth_test.js -e TARGET_URL=https://dev.solra.io

# ============================================================
# Auth Service - Login + Token Refresh
# ============================================================
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

export const options = {
    scenarios: {
        // ---- Smoke Test ----
        smoke: {
            executor: 'constant-vus',
            vus: 5,
            duration: '30s',
            tags: { test_type: 'smoke' },
        },
        // ---- Average Load ----
        average: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '2m',  target: 100 },
                { duration: '30s', target: 0 },
            ],
            tags: { test_type: 'average' },
        },
        // ---- Stress Test ----
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 200 },
                { duration: '3m', target: 200 },
                { duration: '1m', target: 500 },
                { duration: '3m', target: 500 },
                { duration: '1m', target: 0 },
            ],
            tags: { test_type: 'stress' },
        },
        // ---- Soak Test ----
        soak: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30m',
            tags: { test_type: 'soak' },
        },
        // ---- Spike Test ----
        spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 1000 },
                { duration: '30s', target: 1000 },
                { duration: '10s', target: 0 },
            ],
            tags: { test_type: 'spike' },
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'],
        'http_req_failed': ['rate<0.01'],
        'auth_login_success': ['rate>0.99'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

const TARGET = __ENV.TARGET_URL || 'https://dev.solra.io';
const PARAMS = { headers: { 'Content-Type': 'application/json' } };

export default function () {
    group('1. User Login', function () {
        const loginRes = http.post(`${TARGET}/api/v1/auth/login`, JSON.stringify({
            username: `loadtest_user_${__VU % 1000}`,
            password: 'test_pass_123',
        }), PARAMS);

        check(loginRes, {
            'login status 200': (r) => r.status === 200,
            'has access token': (r) => r.json('accessToken') !== '',
            'has refresh token': (r) => r.json('refreshToken') !== '',
        });
    });

    sleep(1);
}

// Teardown
export function handleSummary(data) {
    return {
        'results/auth-summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data),
    };
}
