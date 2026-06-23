import http from 'k6/http';
import { check, group, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 10 },   // Ramp-up to 10 users
    { duration: '3m', target: 50 },   // Ramp-up to 50 users
    { duration: '2m', target: 100 },  // Spike to 100 users
    { duration: '3m', target: 50 },   // Ramp-down to 50 users
    { duration: '1m', target: 0 },    // Ramp-down to 0 users
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500', 'p(99)<1000'],
    'http_req_failed': ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  // Create a test token
  const registerRes = http.post(`${BASE_URL}/api/auth/register`, null, {
    params: {
      username: `k6user_${Date.now()}`,
      password: 'K6Test123!',
      role: 'ADMIN'
    }
  });

  if (registerRes.status === 201) {
    return { token: registerRes.json('token') };
  }
  return { token: 'demo-token' };
}

export default function (data) {
  const token = data.token || 'demo-token';

  group('Authentication', () => {
    const loginRes = http.post(`${BASE_URL}/api/auth/login`, null, {
      params: {
        username: `k6user_${__VU}`,
        password: 'K6Test123!',
      }
    });

    check(loginRes, {
      'login status is 200 or 401': (r) => [200, 401].includes(r.status),
      'response has token or error': (r) => r.body.includes('token') || r.body.includes('error'),
    });

    sleep(1);
  });

  group('Health & Metrics', () => {
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    check(healthRes, {
      'health check passed': (r) => r.status === 200,
      'API is UP': (r) => r.json('status') === 'UP',
    });

    const metricsRes = http.get(`${BASE_URL}/actuator/metrics`);
    check(metricsRes, {
      'metrics endpoint works': (r) => r.status === 200,
    });

    sleep(0.5);
  });

  group('ERP Service - Employee API', () => {
    const empRes = http.post(
      `${BASE_URL}/api/erp/employees`,
      JSON.stringify({
        firstName: 'K6Test',
        lastName: 'User',
        email: `k6_${__VU}_${Date.now()}@test.com`,
        salaryBase: 500000,
        position: 'QA'
      }),
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );

    check(empRes, {
      'employee api responds': (r) => [200, 201, 400, 401].includes(r.status),
    });

    sleep(1);
  });

  group('CRM Service - Order API', () => {
    const orderRes = http.post(
      `${BASE_URL}/api/crm/orders`,
      JSON.stringify({
        customerId: Math.floor(Math.random() * 100),
        items: [
          { productId: 1, quantity: 2 }
        ],
        totalAmount: 45000
      }),
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );

    check(orderRes, {
      'order api responds': (r) => [200, 201, 400, 401].includes(r.status),
    });

    sleep(1);
  });

  group('Supply Chain - Traceability API', () => {
    const batchRes = http.post(
      `${BASE_URL}/api/supply/batches`,
      JSON.stringify({
        plantationId: 1,
        harvestDate: '2026-06-15',
        quantityKg: Math.floor(Math.random() * 2000) + 100,
        qualityGrade: 'A',
        currentLocation: 'Test Zone'
      }),
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );

    check(batchRes, {
      'batch api responds': (r) => [200, 201, 400, 401].includes(r.status),
    });

    sleep(1);
  });

  group('Prometheus Metrics', () => {
    const promRes = http.get(`${BASE_URL}/actuator/prometheus`);
    check(promRes, {
      'prometheus metrics available': (r) => r.status === 200,
      'contains request metrics': (r) => r.body.includes('http_server_requests'),
    });

    sleep(0.5);
  });
}
