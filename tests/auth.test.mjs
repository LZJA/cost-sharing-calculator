import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
const source = readFileSync(new URL('../src/api/costSharingApi.js', import.meta.url), 'utf8')
  .replace(/export function /g, 'function ').replace('export default', 'const api =');

function harness(handler, production = false) {
  const storage = new Map();
  const calls = [];
  const loadingEvents = [];
  let loginCount = 0;
  const dispatch = (options) => {
    calls.push(options);
    Promise.resolve().then(() => handler(options)).then(result => options.success(result), () => options.fail({}));
  };
  const uni = {
    getStorageSync: key => storage.get(key), setStorageSync: (key, value) => storage.set(key, value), removeStorageSync: key => storage.delete(key),
    showToast() { loadingEvents.push("toast"); }, showLoading() { loadingEvents.push("show"); }, hideLoading() { loadingEvents.push("hide"); }, request: dispatch
  };
  const wx = {
    getAccountInfoSync: () => ({ miniProgram: { envVersion: production ? 'release' : 'develop' } }),
    login: options => { loginCount++; Promise.resolve().then(() => options.success({ code: `code-${loginCount}` })); },
    cloud: { callContainer: dispatch }
  };
  return { ...new Function('uni', 'wx', `${source}; return { api, ensureLogin, startAccountSession };`)(uni, wx), calls, storage, loadingEvents, loginCount: () => loginCount };
}
const ok = data => ({ statusCode: 200, data: { code: 200, data } });
const path = options => options.path || options.url;

test('并发首次请求只登录一次，并带上服务端签发的会话', async () => {
  const h = harness(options => path(options).endsWith('/auth/login') ? ok({ token: 'issued-session' }) : ok([]));
  await Promise.all([h.api.calculator.getAll(), h.api.auth.me()]);
  assert.equal(h.loginCount(), 1);
  const authenticated = h.calls.filter(o => !path(o).endsWith('/auth/login'));
  assert.equal(authenticated.length, 2);
  assert.ok(authenticated.every(o => o.header.Authorization === 'Bearer issued-session'));
  assert.ok(h.calls.every(o => !o.header['X-WX-OPENID']));
});

test('并发过期请求共享一次重新登录，禁止继续使用失效会话', async () => {
  const h = harness(options => {
    if (path(options).endsWith('/auth/login')) return ok({ token: 'renewed-session' });
    if (options.header.Authorization === 'Bearer expired') return { statusCode: 401, data: { message: '登录已过期' } };
    return ok([]);
  });
  h.storage.set('cost-sharing-session-v1', 'expired');
  await Promise.all([h.api.calculator.getAll(), h.api.auth.me()]);
  assert.equal(h.loginCount(), 1);
  assert.equal(h.storage.get('cost-sharing-session-v1'), 'renewed-session');
  assert.equal(h.calls.filter(o => o.header.Authorization === 'Bearer renewed-session').length, 2);
});

test('权限拒绝直接报错，不通过重新登录反复重试写操作', async () => {
  const h = harness(() => ({ statusCode: 403, data: { message: '仅创建者可管理' } }));
  h.storage.set('cost-sharing-session-v1', 'valid');
  await assert.rejects(h.api.calculator.delete(42), error => error.status === 403);
  assert.equal(h.loginCount(), 0);
  assert.equal(h.calls.length, 1);
});

test('微信登录失败不会请求私人数据或缓存错误会话', async () => {
  const h = harness(() => ({ statusCode: 503, data: { message: '微信登录暂时不可用' } }));
  await assert.rejects(h.api.calculator.getAll(), /微信登录暂时不可用/);
  assert.equal(h.calls.length, 1);
  assert.equal(h.storage.size, 0);
});

test('正式版请求阿里云域名并携带认证，只接受标准成功响应', async () => {
  const h = harness(options => path(options).endsWith('/auth/login') ? ok({ token: 'cloud-session' }) : ok({ people: [] }), true);
  await h.api.companion.list(7);
  const call=h.calls.at(-1);
  assert.equal(call.url, 'https://cost.higheq.top/api/calculators/7/companions');
  assert.equal(call.header.Authorization, 'Bearer cloud-session');
  assert.equal(call.header['X-WX-SERVICE'], undefined);
});


test('冷启动重新校验微信账号，不沿用上一次账号的本地会话', async () => {
  const h = harness(options => path(options).endsWith('/auth/login') ? ok({token:'current-account'}) : ok([]));
  h.storage.set('cost-sharing-session-v1','previous-account');
  h.startAccountSession();
  await h.api.calculator.getAll();
  assert.equal(h.loginCount(),1);
  assert.equal(h.calls.at(-1).header.Authorization,'Bearer current-account');
});


test('登录失败先结束 loading 再显示 toast，保证显示和关闭配对', async () => {
  const h = harness(() => ({statusCode:503,data:{message:'管理员尚未配置微信登录'}}));
  await assert.rejects(h.api.calculator.getAll(), /管理员尚未配置微信登录/);
  assert.deepEqual(h.loadingEvents,['show','hide','toast']);
});

test('并发失败与成功请求完成后才显示错误，loading 只关闭一次', async () => {
  const h = harness(options => path(options).endsWith('/auth/me')
    ? {statusCode:503,data:{message:'请求暂时不可用'}} : ok([]));
  h.storage.set('cost-sharing-session-v1','valid');
  await Promise.allSettled([h.api.auth.me(),h.api.calculator.getAll()]);
  assert.deepEqual(h.loadingEvents,['show','hide','toast']);
});
