import { test, expect } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

test.describe('DIGITRANS-CM API E2E Tests', () => {
  let authToken = '';
  let userId = '';

  test('User Registration and Login Flow', async ({ request }) => {
    // Register a new user
    const registerResponse = await request.post(`${BASE_URL}/api/auth/register`, {
      params: {
        username: 'testuser_' + Date.now(),
        password: 'TestPassword123!',
        role: 'USER'
      }
    });

    expect(registerResponse.status()).toBe(201);
    const registerData = await registerResponse.json();
    expect(registerData).toHaveProperty('token');
    expect(registerData).toHaveProperty('username');
    expect(registerData).toHaveProperty('expiresIn');
    
    authToken = registerData.token;
    userId = registerData.id;

    console.log('✓ User registered successfully');

    // Login with the same credentials
    const loginResponse = await request.post(`${BASE_URL}/api/auth/login`, {
      params: {
        username: registerData.username,
        password: 'TestPassword123!'
      }
    });

    expect(loginResponse.status()).toBe(200);
    const loginData = await loginResponse.json();
    expect(loginData.token).toBeTruthy();
    expect(loginData.username).toBe(registerData.username);

    console.log('✓ User login successful');
  });

  test('Health Check Endpoints', async ({ request }) => {
    const healthCheck = await request.get(`${BASE_URL}/actuator/health`);
    expect(healthCheck.status()).toBe(200);
    
    const healthData = await healthCheck.json();
    expect(healthData.status).toBe('UP');

    console.log('✓ API Gateway health check passed');
  });

  test('ERP Service - Employee Management', async ({ request }) => {
    if (!authToken) {
      test.skip();
    }

    // Create an employee
    const employeeData = {
      firstName: 'Jean',
      lastName: 'Dupont',
      email: 'jean@digitrans.com',
      salaryBase: 500000,
      position: 'Engineer'
    };

    const createResponse = await request.post(`${BASE_URL}/api/erp/employees`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: employeeData
    });

    expect(createResponse.status()).toBe(201);
    const employee = await createResponse.json();
    expect(employee.firstName).toBe('Jean');
    expect(employee.salaryBase).toBe(500000);

    console.log('✓ Employee created successfully');

    // Retrieve employee
    const getResponse = await request.get(`${BASE_URL}/api/erp/employees/${employee.id}`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    expect(getResponse.status()).toBe(200);
    const retrievedEmployee = await getResponse.json();
    expect(retrievedEmployee.id).toBe(employee.id);

    console.log('✓ Employee retrieved successfully');
  });

  test('CRM Service - Order Management', async ({ request }) => {
    if (!authToken) {
      test.skip();
    }

    const orderData = {
      customerId: 1,
      items: [
        { productId: 1, quantity: 2 },
        { productId: 2, quantity: 1 }
      ],
      totalAmount: 45000
    };

    const createResponse = await request.post(`${BASE_URL}/api/crm/orders`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: orderData
    });

    expect(createResponse.status()).toBe(201);
    const order = await createResponse.json();
    expect(order.totalAmount).toBe(45000);
    expect(order.customerId).toBe(1);

    console.log('✓ Order created successfully');

    // Check order status
    const statusResponse = await request.get(`${BASE_URL}/api/crm/orders/${order.id}/status`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    expect(statusResponse.status()).toBe(200);
    const status = await statusResponse.json();
    expect(status).toHaveProperty('status');

    console.log('✓ Order status retrieved successfully');
  });

  test('Supply Chain Service - Plantation and Batch Traceability', async ({ request }) => {
    if (!authToken) {
      test.skip();
    }

    // Create plantation
    const plantationData = {
      code: 'PLT_' + Date.now(),
      name: 'Cacao Plantation Test',
      region: 'Littoral',
      ownerName: 'Test Farmer',
      productType: 'CACAO',
      surfaceHectares: 50.5
    };

    const plantationResponse = await request.post(`${BASE_URL}/api/supply/plantations`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: plantationData
    });

    expect(plantationResponse.status()).toBe(201);
    const plantation = await plantationResponse.json();
    expect(plantation.code).toBe(plantationData.code);
    expect(plantation.productType).toBe('CACAO');

    console.log('✓ Plantation created successfully');

    // Create harvest batch
    const batchData = {
      plantationId: plantation.id,
      harvestDate: '2026-06-15',
      quantityKg: 1000,
      qualityGrade: 'A',
      currentLocation: 'Plantation Zone 1'
    };

    const batchResponse = await request.post(`${BASE_URL}/api/supply/batches`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: batchData
    });

    expect(batchResponse.status()).toBe(201);
    const batch = await batchResponse.json();
    expect(batch.quantityKg).toBe(1000);
    expect(batch.qualityGrade).toBe('A');

    console.log('✓ Harvest batch created successfully');

    // Get batch traceability
    const traceResponse = await request.get(`${BASE_URL}/api/supply/batches/${batch.batchCode}/trace`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    if (traceResponse.ok()) {
      const traceData = await traceResponse.json();
      expect(Array.isArray(traceData)).toBe(true);
      console.log('✓ Batch traceability retrieved successfully');
    }

    // Verify batch integrity
    const verifyResponse = await request.get(`${BASE_URL}/api/supply/batches/${batch.batchCode}/verify`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    if (verifyResponse.ok()) {
      const verifyData = await verifyResponse.json();
      expect(verifyData).toHaveProperty('verified');
      console.log('✓ Batch integrity verified successfully');
    }
  });

  test('Loyalty Points Management', async ({ request }) => {
    if (!authToken) {
      test.skip();
    }

    const pointsResponse = await request.post(`${BASE_URL}/api/crm/loyalty/points/add`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      },
      params: {
        customerId: '1',
        points: '50'
      }
    });

    expect(pointsResponse.status()).toBe(200);
    console.log('✓ Loyalty points added successfully');
  });

  test('Metrics and Monitoring', async ({ request }) => {
    const metricsResponse = await request.get(`${BASE_URL}/actuator/metrics`);
    expect(metricsResponse.status()).toBe(200);

    const metricsData = await metricsResponse.json();
    expect(metricsData).toHaveProperty('names');
    expect(Array.isArray(metricsData.names)).toBe(true);

    console.log('✓ Metrics endpoint accessible');

    // Check Prometheus metrics
    const prometheusResponse = await request.get(`${BASE_URL}/actuator/prometheus`);
    expect(prometheusResponse.status()).toBe(200);

    const prometheusText = await prometheusResponse.text();
    expect(prometheusText).toContain('http_server_requests');

    console.log('✓ Prometheus metrics exposed correctly');
  });

  test('API Documentation', async ({ request }) => {
    const swaggerResponse = await request.get(`${BASE_URL}/swagger-ui.html`);
    expect(swaggerResponse.status()).toBe(200);

    console.log('✓ Swagger UI accessible');

    const apiDocsResponse = await request.get(`${BASE_URL}/v3/api-docs`);
    expect(apiDocsResponse.status()).toBe(200);

    const apiDocs = await apiDocsResponse.json();
    expect(apiDocs).toHaveProperty('openapi');
    expect(apiDocs).toHaveProperty('paths');

    console.log('✓ OpenAPI documentation available');
  });

  test('Authorization - Access Protected Resource Without Token', async ({ request }) => {
    const unauthorizedResponse = await request.get(`${BASE_URL}/api/erp/employees`);
    expect(unauthorizedResponse.status()).toBe(401);

    console.log('✓ Protected endpoints properly secured');
  });

  test('CORS Configuration', async ({ request }) => {
    const corsResponse = await request.options(`${BASE_URL}/api/auth/login`, {
      headers: {
        'Origin': 'http://localhost:3000',
        'Access-Control-Request-Method': 'POST'
      }
    });

    expect(corsResponse.status()).toBeLessThan(300);
    console.log('✓ CORS configured correctly');
  });
});
