import http from 'k6/http';
import { check, fail, group, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/3.0.4/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

/**
 * Performance test for the Spring Boot GIS API.
 *
 * Safe defaults:
 *   - PROFILE=smoke verifies the read API surface.
 *   - Writes require ALLOW_WRITES=true and an isolated staging database.
 *   - Passwords have no defaults; pass them through environment variables.
 *
 * PowerShell examples:
 *   $env:VIEWER_PASS='<viewer-password>'
 *   $env:ADMIN_PASS='<admin-password>'
 *   k6 run -e PROFILE=smoke -e FEATURES=auto perf-test.js
 *   k6 run -e PROFILE=load -e TARGET_VUS=50 -e FEATURES=ocop perf-test.js
 *   k6 run -e PROFILE=capacity -e TARGET_VUS=500 perf-test.js
 *   k6 run -e PROFILE=throughput -e TARGET_RPS=50 perf-test.js
 *   k6 run -e PROFILE=geojson -e TARGET_VUS=100 perf-test.js
 *
 * 300-500 office users do not automatically mean 300-500 requests/second.
 * PROFILE=load models about 10% of 500 users as active concurrent sessions.
 * PROFILE=capacity is the deliberate worst case of up to 500 active sessions.
 */

const FEATURE_NAMES = ['ocop', 'science', 'agriculture'];
const COOKIE_NAME = __ENV.COOKIE_NAME || 'gis_token';
const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
const PROFILE = (__ENV.PROFILE || 'smoke').trim().toLowerCase();
const FEATURES_SETTING = (__ENV.FEATURES || 'auto').trim().toLowerCase();
const VIEWER_USER = __ENV.VIEWER_USER || 'viewer';
const VIEWER_PASS = __ENV.VIEWER_PASS || '';
const VIEWER_TOKEN = __ENV.VIEWER_TOKEN || '';
const ADMIN_USER = __ENV.ADMIN_USER || 'admin';
const ADMIN_PASS = __ENV.ADMIN_PASS || '';
const ADMIN_TOKEN = __ENV.ADMIN_TOKEN || '';
const ALLOW_WRITES = booleanEnv('ALLOW_WRITES', false);
const TARGET_VUS = integerEnv('TARGET_VUS', PROFILE === 'capacity' ? 500 : 50, 1);
const TARGET_RPS = integerEnv('TARGET_RPS', 50, 1);
const THINK_TIME_MIN = numberEnv('THINK_TIME_MIN', PROFILE === 'quick' ? 0.5 : 3, 0);
const THINK_TIME_MAX = numberEnv('THINK_TIME_MAX', PROFILE === 'quick' ? 1.5 : 8, THINK_TIME_MIN);
const RAMP_UP = __ENV.RAMP_UP || (PROFILE === 'quick' ? '10s' : '1m');
const STEADY = __ENV.STEADY || (PROFILE === 'quick' ? '30s' : '5m');
const RAMP_DOWN = __ENV.RAMP_DOWN || (PROFILE === 'quick' ? '10s' : '1m');
const SOAK_DURATION = __ENV.SOAK_DURATION || '2h';
const REPORT_PREFIX = __ENV.REPORT_PREFIX || 'perf-summary';
const REPRESENTATIVE_FILE = normalizeFilePath(__ENV.FILE_PATH || '');
const PROBE_EXPECTED_STATUSES = http.expectedStatuses(200, 404);

const ApiErrors = new Counter('gis_api_errors');
const MapLayerDuration = new Trend('gis_map_layer_duration', true);

function booleanEnv(name, fallback) {
  const raw = __ENV[name];
  if (raw === undefined || raw === '') return fallback;
  return ['1', 'true', 'yes', 'on'].includes(String(raw).trim().toLowerCase());
}

function integerEnv(name, fallback, minimum) {
  const value = Number(__ENV[name] || fallback);
  if (!Number.isInteger(value) || value < minimum) {
    throw new Error(`${name} must be an integer >= ${minimum}`);
  }
  return value;
}

function numberEnv(name, fallback, minimum) {
  const value = Number(__ENV[name] || fallback);
  if (!Number.isFinite(value) || value < minimum) {
    throw new Error(`${name} must be a number >= ${minimum}`);
  }
  return value;
}

function createScenarios() {
  if (PROFILE === 'smoke') {
    return {
      api_coverage_smoke: {
        executor: 'shared-iterations',
        vus: 1,
        iterations: 1,
        maxDuration: '2m',
        exec: 'apiCoverageSmoke',
      },
    };
  }

  if (PROFILE === 'quick' || PROFILE === 'load') {
    return {
      user_journey: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: RAMP_UP, target: Math.max(1, Math.ceil(TARGET_VUS / 2)) },
          { duration: RAMP_UP, target: TARGET_VUS },
          { duration: STEADY, target: TARGET_VUS },
          { duration: RAMP_DOWN, target: 0 },
        ],
        gracefulRampDown: '15s',
        gracefulStop: '30s',
        exec: 'userJourney',
      },
    };
  }

  if (PROFILE === 'capacity') {
    return {
      user_journey: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: __ENV.CAPACITY_WARMUP || '2m', target: Math.min(50, TARGET_VUS) },
          { duration: __ENV.CAPACITY_150 || '3m', target: Math.min(150, TARGET_VUS) },
          { duration: __ENV.CAPACITY_300 || '5m', target: Math.min(300, TARGET_VUS) },
          { duration: __ENV.CAPACITY_PEAK || '10m', target: TARGET_VUS },
          { duration: __ENV.CAPACITY_COOLDOWN || '2m', target: 0 },
        ],
        gracefulRampDown: '30s',
        gracefulStop: '1m',
        exec: 'userJourney',
      },
    };
  }

  if (PROFILE === 'throughput') {
    return {
      request_throughput: {
        executor: 'ramping-arrival-rate',
        startRate: Math.max(1, Math.ceil(TARGET_RPS / 10)),
        timeUnit: '1s',
        preAllocatedVUs: Math.max(20, TARGET_RPS),
        maxVUs: Math.max(TARGET_VUS, TARGET_RPS * 5),
        stages: [
          { duration: RAMP_UP, target: Math.max(1, Math.ceil(TARGET_RPS / 2)) },
          { duration: RAMP_UP, target: TARGET_RPS },
          { duration: STEADY, target: TARGET_RPS },
          { duration: RAMP_DOWN, target: 0 },
        ],
        gracefulStop: '30s',
        exec: 'throughputJourney',
      },
    };
  }

  if (PROFILE === 'geojson') {
    return {
      geojson_cold_burst: {
        executor: 'per-vu-iterations',
        vus: TARGET_VUS,
        iterations: 1,
        maxDuration: '3m',
        exec: 'geoJsonBurst',
      },
    };
  }

  if (PROFILE === 'soak') {
    return {
      user_journey: {
        executor: 'constant-vus',
        vus: TARGET_VUS,
        duration: SOAK_DURATION,
        gracefulStop: '1m',
        exec: 'userJourney',
      },
    };
  }

  throw new Error(
    `Unknown PROFILE '${PROFILE}'. Use smoke, quick, load, capacity, throughput, geojson, or soak.`,
  );
}

const scenarios = createScenarios();
if (ALLOW_WRITES) {
  scenarios.write_smoke = {
    executor: 'per-vu-iterations',
    vus: 1,
    iterations: 1,
    maxDuration: '2m',
    exec: 'writeSmoke',
  };
}

function createThresholds() {
  const thresholds = {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{request_type:api}': ['p(95)<500', 'p(99)<1500'],
    'http_req_duration{request_type:spatial}': ['p(95)<750', 'p(99)<1500'],
    'http_req_duration{request_type:auth}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{request_type:geojson}': ['p(95)<2500', 'p(99)<5000'],
    'http_req_duration{request_type:admin}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{request_type:file}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{request_type:write}': ['p(95)<1500', 'p(99)<3000'],
    'http_req_duration{endpoint:wards_geojson}': ['p(95)<2500', 'p(99)<5000'],
  };

  const guardedEndpoints = [
    'auth_login', 'auth_me', 'auth_logout', 'wards_list', 'wards_search',
    'ward_detail', 'ward_geojson', 'province_geojson', 'wards_geojson',
    'admin_users_list', 'file_download',
  ];
  const thresholdFeatures = FEATURES_SETTING === 'auto' ? [] : configuredFeatures();
  thresholdFeatures.forEach((feature) => {
    guardedEndpoints.push(
      `${feature}_list`, `${feature}_detail`, `${feature}_nearby`, `${feature}_geojson`,
    );
  });
  guardedEndpoints.forEach((endpoint) => {
    thresholds[`http_req_failed{endpoint:${endpoint}}`] = ['rate<0.01'];
  });
  if (['quick', 'load', 'capacity', 'soak'].includes(PROFILE)) {
    thresholds.gis_map_layer_duration = ['p(95)<3000', 'p(99)<5500'];
  }
  if (PROFILE === 'smoke') thresholds.gis_api_errors = ['count==0'];
  if (PROFILE === 'throughput') thresholds.dropped_iterations = ['count==0'];
  return thresholds;
}

export const options = {
  scenarios,
  discardResponseBodies: true,
  batchPerHost: 20,
  userAgent: `gis-k6-performance/${PROFILE}`,
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  summaryTimeUnit: 'ms',
  tags: { profile: PROFILE },
  thresholds: createThresholds(),
};

function requestHeaders(token, hasJsonBody) {
  const headers = {
    Accept: 'application/json, application/geo+json, */*',
    'Accept-Encoding': 'gzip, deflate, br',
  };
  if (token) headers.Cookie = `${COOKIE_NAME}=${token}`;
  if (hasJsonBody) headers['Content-Type'] = 'application/json';
  return headers;
}

function apiRequest(method, path, token, config) {
  const cfg = config || {};
  const endpoint = cfg.endpoint || path;
  const requestType = cfg.requestType || 'api';
  const expectedStatus = cfg.expectedStatus === undefined ? 200 : cfg.expectedStatus;
  const hasBody = cfg.body !== undefined && cfg.body !== null;
  const params = {
    headers: requestHeaders(token, hasBody),
    tags: {
      name: `${method} ${endpoint}`,
      endpoint,
      request_type: requestType,
    },
    timeout: cfg.timeout || (requestType === 'geojson' ? '30s' : '10s'),
    responseType: cfg.captureBody ? 'text' : 'none',
  };
  const body = hasBody ? JSON.stringify(cfg.body) : null;
  const response = http.request(method, `${BASE_URL}${path}`, body, params);
  const ok = check(
    response,
    { [`${method} ${endpoint} -> ${expectedStatus}`]: (r) => r.status === expectedStatus },
    { endpoint, request_type: requestType },
  );
  if (!ok) ApiErrors.add(1, { endpoint });
  return response;
}

function login(username, password, endpoint) {
  if (!username || !password) return null;
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username, password }),
    {
      headers: requestHeaders('', true),
      tags: {
        name: 'POST /api/auth/login',
        endpoint: 'auth_login',
        login_kind: endpoint || 'default',
        request_type: 'auth',
      },
      timeout: '10s',
      responseType: 'none',
    },
  );
  const ok = check(
    response,
    { 'POST auth_login -> 200': (r) => r.status === 200 },
    { endpoint: 'auth_login', request_type: 'auth' },
  );
  if (!ok) {
    ApiErrors.add(1, { endpoint: 'auth_login' });
    return null;
  }
  return extractCookie(response);
}

function extractCookie(response) {
  const values = response.cookies && response.cookies[COOKIE_NAME];
  if (values && values.length > 0 && values[0].value) return values[0].value;
  const setCookie = response.headers['Set-Cookie'] || response.headers['set-cookie'] || '';
  const marker = `${COOKIE_NAME}=`;
  const start = setCookie.indexOf(marker);
  if (start < 0) return null;
  const valueStart = start + marker.length;
  const valueEnd = setCookie.indexOf(';', valueStart);
  return setCookie.slice(valueStart, valueEnd < 0 ? setCookie.length : valueEnd);
}

function parseJson(response, label) {
  try {
    return response.json();
  } catch (error) {
    ApiErrors.add(1, { endpoint: label });
    console.error(`[DATA] ${label} did not return valid JSON: ${String(error)}`);
    return null;
  }
}

function configuredFeatures() {
  if (FEATURES_SETTING === 'auto') return FEATURE_NAMES;
  if (FEATURES_SETTING === 'none' || FEATURES_SETTING === '') return [];
  const names = FEATURES_SETTING.split(',').map((name) => name.trim()).filter(Boolean);
  names.forEach((name) => {
    if (!FEATURE_NAMES.includes(name)) {
      throw new Error(`Unknown feature '${name}'. Valid values: auto, none, ${FEATURE_NAMES.join(',')}`);
    }
  });
  return names;
}

function normalizeFeatureSample(item) {
  return {
    id: item.id,
    wardCode: item.wardCode || '',
    latitude: item.latitude === null || item.latitude === undefined ? null : Number(item.latitude),
    longitude: item.longitude === null || item.longitude === undefined ? null : Number(item.longitude),
    imageUrl: normalizeFilePath(item.imageUrl || ''),
  };
}

function normalizeFilePath(value) {
  if (!value) return '';
  const text = String(value).trim();
  if (text.startsWith('/api/files/')) return text;
  const absolutePrefix = `${BASE_URL}/api/files/`;
  if (text.startsWith(absolutePrefix)) return text.slice(BASE_URL.length);
  return '';
}

function addUnique(items, value) {
  if (value && items.indexOf(value) < 0) items.push(value);
}

export function setup() {
  console.log(`[SETUP] profile=${PROFILE}, baseUrl=${BASE_URL}, features=${FEATURES_SETTING}`);
  const health = http.get(`${BASE_URL}/actuator/health`, {
    tags: { name: 'GET /actuator/health', endpoint: 'health', request_type: 'health' },
    timeout: '5s',
    responseType: 'text',
  });
  if (!check(health, { 'backend health is 200': (r) => r.status === 200 })) {
    fail(`Backend is unavailable at ${BASE_URL}/actuator/health (status=${health.status})`);
  }

  let viewerToken = VIEWER_TOKEN;
  if (!viewerToken) {
    if (!VIEWER_PASS) fail('Set VIEWER_PASS or VIEWER_TOKEN before running k6.');
    viewerToken = login(VIEWER_USER, VIEWER_PASS, 'setup_viewer_login');
  }
  if (!viewerToken) fail(`Could not obtain cookie '${COOKIE_NAME}' for viewer '${VIEWER_USER}'.`);

  let adminToken = ADMIN_TOKEN;
  if (!adminToken && ADMIN_PASS) adminToken = login(ADMIN_USER, ADMIN_PASS, 'setup_admin_login');
  if (ALLOW_WRITES && !adminToken) {
    fail('ALLOW_WRITES=true requires ADMIN_PASS or ADMIN_TOKEN. Use only an isolated staging database.');
  }

  const wardResponse = apiRequest('GET', '/api/wards', viewerToken, {
    endpoint: 'wards_list_setup', captureBody: true,
  });
  if (wardResponse.status !== 200) fail('Cannot build test data because GET /api/wards failed.');
  const wards = parseJson(wardResponse, 'wards_list_setup');
  if (!Array.isArray(wards) || wards.length === 0) fail('GET /api/wards returned no ward data.');
  const wardCodes = wards.map((ward) => ward.code).filter(Boolean);
  const wardQueries = wards.map((ward) => ward.name || ward.fullName || '').filter(Boolean).slice(0, 30);

  const explicitFeatureSelection = FEATURES_SETTING !== 'auto';
  const features = [];
  const filePaths = [];
  addUnique(filePaths, REPRESENTATIVE_FILE);
  configuredFeatures().forEach((name) => {
    const response = http.get(`${BASE_URL}/api/${name}?page=0&size=100&sort=name,asc`, {
      headers: requestHeaders(viewerToken, false),
      tags: { name: `GET /api/${name}`, endpoint: `${name}_probe`, request_type: 'setup' },
      timeout: '10s',
      responseType: 'text',
      responseCallback: PROBE_EXPECTED_STATUSES,
    });
    if (response.status === 404 && !explicitFeatureSelection) {
      console.log(`[SETUP] feature '${name}' is disabled; its routes will be skipped.`);
      return;
    }
    if (response.status !== 200) {
      fail(`Feature '${name}' was requested but GET /api/${name} returned ${response.status}.`);
    }
    const page = parseJson(response, `${name}_probe`);
    const content = page && Array.isArray(page.content) ? page.content : [];
    const samples = content.slice(0, 100).map(normalizeFeatureSample);
    samples.forEach((sample) => addUnique(filePaths, sample.imageUrl));
    features.push({ name, samples });
    console.log(`[SETUP] feature '${name}' enabled, sampled ${samples.length} row(s).`);
  });

  console.log(
    `[SETUP] ready: wards=${wardCodes.length}, activeFeatures=${features.map((f) => f.name).join(',') || 'none'}, ` +
      `admin=${adminToken ? 'yes' : 'no'}, representativeFiles=${filePaths.length}`,
  );
  return { viewerToken, adminToken, wardCodes, wardQueries, features, filePaths };
}

const vuSession = {
  token: '',
  provinceLoaded: false,
  wardsLayerLoaded: false,
  featureLayersLoaded: {},
};

function initializeVuSession(data) {
  if (vuSession.token) return;
  vuSession.token = VIEWER_PASS ? login(VIEWER_USER, VIEWER_PASS, 'viewer_session_login') : data.viewerToken;
  if (!vuSession.token) vuSession.token = data.viewerToken;
  if (!vuSession.token) fail('Viewer session could not be initialized.');
  apiRequest('GET', '/api/auth/me', vuSession.token, { endpoint: 'auth_me', requestType: 'auth' });
}

function randomItem(items) {
  if (!items || items.length === 0) return null;
  return items[Math.floor(Math.random() * items.length)];
}

function randomBetween(minimum, maximum) {
  return minimum + Math.random() * (maximum - minimum);
}

function mapJourney(data) {
  const started = Date.now();
  let response = null;
  group('map_layers', () => {
    if (!vuSession.provinceLoaded) {
      response = apiRequest('GET', '/api/wards/province/geojson', vuSession.token, {
        endpoint: 'province_geojson', requestType: 'geojson',
      });
      if (response.status === 200) vuSession.provinceLoaded = true;
      return;
    }
    if (!vuSession.wardsLayerLoaded) {
      response = apiRequest('GET', '/api/wards/geojson', vuSession.token, {
        endpoint: 'wards_geojson', requestType: 'geojson', timeout: '30s',
      });
      if (response.status === 200) vuSession.wardsLayerLoaded = true;
      return;
    }
    const feature = randomItem(data.features);
    if (feature && !vuSession.featureLayersLoaded[feature.name]) {
      response = apiRequest('GET', `/api/${feature.name}/geojson`, vuSession.token, {
        endpoint: `${feature.name}_geojson`, requestType: 'geojson',
      });
      if (response.status === 200) vuSession.featureLayersLoaded[feature.name] = true;
    }
  });
  if (response) MapLayerDuration.add(Date.now() - started);
}

function wardBrowseJourney(data) {
  const wardCode = randomItem(data.wardCodes);
  const roll = Math.random();
  if (roll < 0.15) {
    apiRequest('GET', '/api/wards', vuSession.token, { endpoint: 'wards_list' });
  } else if (roll < 0.25) {
    const query = randomItem(data.wardQueries) || 'Ia';
    apiRequest('GET', `/api/wards?q=${encodeURIComponent(query)}`, vuSession.token, { endpoint: 'wards_search' });
  } else if (roll < 0.85) {
    apiRequest('GET', `/api/wards/${encodeURIComponent(wardCode)}`, vuSession.token, { endpoint: 'ward_detail' });
  } else {
    apiRequest('GET', `/api/wards/${encodeURIComponent(wardCode)}/geojson`, vuSession.token, {
      endpoint: 'ward_geojson', requestType: 'geojson',
    });
  }
}

function featureBrowseJourney(data) {
  const feature = randomItem(data.features);
  if (!feature) {
    wardBrowseJourney(data);
    return;
  }
  const sample = randomItem(feature.samples);
  const roll = Math.random();
  if (roll < 0.35) {
    const wardFilter = sample && sample.wardCode ? `&wardCode=${encodeURIComponent(sample.wardCode)}` : '';
    apiRequest('GET', `/api/${feature.name}?page=0&size=20&sort=name,asc${wardFilter}`, vuSession.token, {
      endpoint: `${feature.name}_list`,
    });
  } else if (roll < 0.60 && sample) {
    apiRequest('GET', `/api/${feature.name}/${sample.id}`, vuSession.token, {
      endpoint: `${feature.name}_detail`,
    });
  } else if (roll < 0.88 && sample && Number.isFinite(sample.latitude) && Number.isFinite(sample.longitude)) {
    const lat = sample.latitude + randomBetween(-0.002, 0.002);
    const lng = sample.longitude + randomBetween(-0.002, 0.002);
    apiRequest(
      'GET',
      `/api/${feature.name}/nearby?lat=${lat.toFixed(6)}&lng=${lng.toFixed(6)}&radiusKm=5`,
      vuSession.token,
      { endpoint: `${feature.name}_nearby`, requestType: 'spatial' },
    );
  } else if (!vuSession.featureLayersLoaded[feature.name]) {
    const response = apiRequest('GET', `/api/${feature.name}/geojson`, vuSession.token, {
      endpoint: `${feature.name}_geojson`, requestType: 'geojson',
    });
    if (response.status === 200) vuSession.featureLayersLoaded[feature.name] = true;
  } else {
    apiRequest('GET', `/api/${feature.name}?page=0&size=20&sort=name,asc`, vuSession.token, {
      endpoint: `${feature.name}_list`,
    });
  }
}

function sessionJourney(data) {
  if (data.adminToken && Math.random() < 0.15) {
    apiRequest('GET', '/api/admin/users', data.adminToken, {
      endpoint: 'admin_users_list', requestType: 'admin',
    });
    return;
  }
  apiRequest('GET', '/api/auth/me', vuSession.token, { endpoint: 'auth_me', requestType: 'auth' });
  if (Math.random() < 0.10) {
    apiRequest('POST', '/api/auth/logout', vuSession.token, { endpoint: 'auth_logout', requestType: 'auth' });
    vuSession.token = '';
  }
}

function representativeFileJourney(data) {
  const filePath = randomItem(data.filePaths);
  if (!filePath) {
    featureBrowseJourney(data);
    return;
  }
  apiRequest('GET', filePath, vuSession.token, { endpoint: 'file_download', requestType: 'file' });
}

export function userJourney(data) {
  initializeVuSession(data);
  const roll = Math.random();
  // Distribution is per interaction. Stateful flags ensure large layers are fetched
  // only once per VU/browser session instead of once per loop.
  if (roll < 0.20) mapJourney(data);
  else if (roll < 0.55) wardBrowseJourney(data);
  else if (roll < 0.90) featureBrowseJourney(data);
  else if (roll < 0.97) sessionJourney(data);
  else representativeFileJourney(data);
  sleep(randomBetween(THINK_TIME_MIN, THINK_TIME_MAX));
}

// Open-model request workload. Use telemetry to replace the default 50 RPS and
// request mix; unlike the VU profiles it exposes queueing through dropped_iterations.
export function throughputJourney(data) {
  vuSession.token = data.viewerToken;
  const roll = Math.random();
  if (roll < 0.03) {
    apiRequest('GET', '/api/wards/province/geojson', data.viewerToken, {
      endpoint: 'province_geojson', requestType: 'geojson',
    });
  } else if (roll < 0.05) {
    apiRequest('GET', '/api/wards/geojson', data.viewerToken, {
      endpoint: 'wards_geojson', requestType: 'geojson', timeout: '30s',
    });
  } else if (roll < 0.45) {
    wardBrowseJourney(data);
  } else if (roll < 0.92) {
    featureBrowseJourney(data);
  } else {
    apiRequest('GET', '/api/auth/me', data.viewerToken, { endpoint: 'auth_me', requestType: 'auth' });
  }
}

// Deliberate simultaneous cold-layer burst. Do not point a large TARGET_VUS at
// production without an agreed test window.
export function geoJsonBurst(data) {
  apiRequest('GET', '/api/wards/geojson', data.viewerToken, {
    endpoint: 'wards_geojson', requestType: 'geojson', timeout: '60s',
  });
}

export function apiCoverageSmoke(data) {
  const token = data.viewerToken;
  const wardCode = data.wardCodes[0];
  const query = data.wardQueries[0] || 'Ia';
  group('core_read_api', () => {
    apiRequest('GET', '/api/auth/me', token, { endpoint: 'auth_me', requestType: 'auth' });
    apiRequest('GET', '/api/wards', token, { endpoint: 'wards_list' });
    apiRequest('GET', `/api/wards?q=${encodeURIComponent(query)}`, token, { endpoint: 'wards_search' });
    apiRequest('GET', `/api/wards/${encodeURIComponent(wardCode)}`, token, { endpoint: 'ward_detail' });
    apiRequest('GET', `/api/wards/${encodeURIComponent(wardCode)}/geojson`, token, {
      endpoint: 'ward_geojson', requestType: 'geojson',
    });
    apiRequest('GET', '/api/wards/province/geojson', token, {
      endpoint: 'province_geojson', requestType: 'geojson',
    });
    const wardsGeoJson = apiRequest('GET', '/api/wards/geojson', token, {
      endpoint: 'wards_geojson', requestType: 'geojson', timeout: '30s', captureBody: true,
    });
    const validGeoJson = check(
      wardsGeoJson,
      { 'wards GeoJSON is a FeatureCollection': (r) => Boolean(r.body && r.body.includes('FeatureCollection')) },
      { endpoint: 'wards_geojson', request_type: 'geojson' },
    );
    if (!validGeoJson) ApiErrors.add(1, { endpoint: 'wards_geojson' });
  });
  data.features.forEach((feature) => {
    group(`${feature.name}_read_api`, () => {
      const sample = feature.samples[0] || null;
      const wardFilter = sample && sample.wardCode ? `&wardCode=${encodeURIComponent(sample.wardCode)}` : '';
      apiRequest('GET', `/api/${feature.name}?page=0&size=20&sort=name,asc${wardFilter}`, token, {
        endpoint: `${feature.name}_list`,
      });
      const featureGeoJson = apiRequest('GET', `/api/${feature.name}/geojson`, token, {
        endpoint: `${feature.name}_geojson`, requestType: 'geojson', captureBody: true,
      });
      const validFeatureGeoJson = check(
        featureGeoJson,
        { [`${feature.name} GeoJSON is a FeatureCollection`]: (r) => Boolean(r.body && r.body.includes('FeatureCollection')) },
        { endpoint: `${feature.name}_geojson`, request_type: 'geojson' },
      );
      if (!validFeatureGeoJson) ApiErrors.add(1, { endpoint: `${feature.name}_geojson` });
      if (sample) {
        apiRequest('GET', `/api/${feature.name}/${sample.id}`, token, {
          endpoint: `${feature.name}_detail`,
        });
        if (Number.isFinite(sample.latitude) && Number.isFinite(sample.longitude)) {
          const nearby = apiRequest(
            'GET',
            `/api/${feature.name}/nearby?lat=${sample.latitude}&lng=${sample.longitude}&radiusKm=5`,
            token,
            { endpoint: `${feature.name}_nearby`, requestType: 'spatial', captureBody: true },
          );
          const nearbyItems = parseJson(nearby, `${feature.name}_nearby`);
          const validNearby = check(
            nearby,
            { [`${feature.name} nearby returns matching rows`]: () => Array.isArray(nearbyItems) && nearbyItems.length > 0 },
            { endpoint: `${feature.name}_nearby`, request_type: 'spatial' },
          );
          if (!validNearby) ApiErrors.add(1, { endpoint: `${feature.name}_nearby` });
        }
      }
    });
  });
  data.filePaths.forEach((filePath) => {
    apiRequest('GET', filePath, token, { endpoint: 'file_download', requestType: 'file' });
  });
  if (data.adminToken) {
    apiRequest('GET', '/api/admin/users', data.adminToken, {
      endpoint: 'admin_users_list', requestType: 'admin',
    });
  }
  apiRequest('POST', '/api/auth/logout', token, { endpoint: 'auth_logout', requestType: 'auth' });
}

function writeFeatureLifecycle(feature, data, suffix) {
  const sample = feature.samples[0] || {};
  const wardCode = sample.wardCode || data.wardCodes[0];
  const latitude = Number.isFinite(sample.latitude) ? sample.latitude : 13.9833;
  const longitude = Number.isFinite(sample.longitude) ? sample.longitude : 108.0;
  let createBody;
  if (feature.name === 'ocop') {
    createBody = {
      name: `k6 OCOP ${suffix}`,
      productTypes: ['performance-test'],
      starRating: 3,
      contactPhone: '0900000000',
      locationAddress: 'k6 isolated staging test',
      wardCode, latitude, longitude, imageUrl: null,
    };
  } else {
    createBody = {
      name: `k6 ${feature.name} ${suffix}`,
      unitType: 'performance-test',
      description: 'Temporary k6 row; safe to delete',
      wardCode, latitude, longitude, imageUrl: null,
    };
  }
  const created = apiRequest('POST', `/api/${feature.name}`, data.adminToken, {
    endpoint: 'feature_create', requestType: 'write', expectedStatus: 201,
    body: createBody, captureBody: true,
  });
  if (created.status !== 201) return;
  const createdBody = parseJson(created, 'feature_create');
  const id = createdBody && createdBody.id;
  if (!id) return;
  try {
    const updateBody = feature.name === 'ocop'
      ? Object.assign({}, createBody, { name: `${createBody.name} updated` })
      : { name: `${createBody.name} updated` };
    apiRequest('PUT', `/api/${feature.name}/${id}`, data.adminToken, {
      endpoint: 'feature_update', requestType: 'write', body: updateBody,
    });
  } finally {
    apiRequest('DELETE', `/api/${feature.name}/${id}`, data.adminToken, {
      endpoint: 'feature_delete', requestType: 'write',
    });
  }
}

export function writeSmoke(data) {
  if (!ALLOW_WRITES) fail('writeSmoke is disabled. Set ALLOW_WRITES=true only on isolated staging.');
  if (!data.adminToken) fail('writeSmoke requires an admin token.');
  const suffix = `${Date.now()}-${__VU}`;
  const createdUser = apiRequest('POST', '/api/admin/users', data.adminToken, {
    endpoint: 'admin_user_create', requestType: 'write', expectedStatus: 201,
    body: {
      username: `k6_${suffix}`,
      password: `K6safe-${suffix}`,
      fullName: `k6 temporary ${suffix}`,
    },
    captureBody: true,
  });
  if (createdUser.status === 201) {
    const user = parseJson(createdUser, 'admin_user_create');
    if (user && user.id) {
      try {
        apiRequest('PUT', `/api/admin/users/${user.id}`, data.adminToken, {
          endpoint: 'admin_user_update', requestType: 'write',
          body: { fullName: `k6 updated ${suffix}`, password: '' },
        });
      } finally {
        apiRequest('DELETE', `/api/admin/users/${user.id}`, data.adminToken, {
          endpoint: 'admin_user_delete', requestType: 'write',
        });
      }
    }
  }
  data.features.forEach((feature) => writeFeatureLifecycle(feature, data, suffix));
}

export function handleSummary(data) {
  const metadata = {
    generatedAt: new Date().toISOString(),
    profile: PROFILE,
    baseUrl: BASE_URL,
    targetVus: PROFILE === 'smoke' ? 1 : TARGET_VUS,
    targetRps: PROFILE === 'throughput' ? TARGET_RPS : null,
    featureSelection: FEATURES_SETTING,
    writesEnabled: ALLOW_WRITES,
  };
  // k6 includes setup_data in the summary object. It contains the JWTs returned
  // by setup(), so never pass it to reporters or persist it to disk.
  const safeData = {};
  Object.keys(data).forEach((key) => {
    if (key !== 'setup_data') safeData[key] = data[key];
  });
  const jsonData = Object.assign({ metadata }, safeData);
  return {
    [`${REPORT_PREFIX}.html`]: htmlReport(safeData),
    [`${REPORT_PREFIX}.json`]: JSON.stringify(jsonData, null, 2),
    stdout: textSummary(safeData, { indent: ' ', enableColors: true }),
  };
}
