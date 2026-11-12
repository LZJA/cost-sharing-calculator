import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { ref } from 'vue';
function page(file, api, exposed, uni = {}) {
  let load, share;
  const source = readFileSync(new URL(`../src/pages/${file}/index.vue`, import.meta.url), 'utf8')
    .match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import\s[\s\S]*?from\s+["'][^"']+["'];?\s*/gm, '');
  const deps = { ref, api, uni: { showToast() {}, ...uni }, onLoad: fn => { load=fn; }, onShareAppMessage: fn => { share=fn; } };
  const state = new Function(...Object.keys(deps), `${source}; return {${exposed}};`)(...Object.values(deps));
  return { ...state, load: options => load(options), share: event => share(event) };
}

test('打开邀请只检查信息，明确确认后才写入同伴关系；重复点击仅加入一次', async () => {
  let accepts=0, renamed='', redirected='';
  const p=page('invitation', {
    companion: { preview: async () => ({ calculatorName:'共同开支',ownerName:'李子',alreadyJoined:false }), accept: async () => { accepts++; return 7; } },
    auth: { me: async () => ({displayName:'鸽子'}), rename: async name => { renamed=name; } }
  }, 'accept, invitation, displayName', { redirectTo: options => { redirected=options.url; } });
  await p.load({token:'invitation-token'});
  // onLoad 开始异步检查，但不承诺等待 UI 更新。
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(accepts,0);
  assert.equal(p.invitation.value.calculatorName,'共同开支');
  await Promise.all([p.accept(),p.accept()]);
  assert.equal(renamed,'鸽子');
  assert.equal(accepts,1);
  assert.equal(redirected,'/pages/calculator/index?id=7');
});

test('邀请失效或未填名字时不会提交加入请求', async () => {
  let accepts=0;
  const p=page('invitation', {
    companion: { preview: async () => { throw new Error('邀请已失效'); }, accept: async () => { accepts++; } },
    auth: { me: async () => ({displayName:'微信用户'}) }
  }, 'accept, invitation, error');
  await p.load({token:'expired'}); await new Promise(resolve => setImmediate(resolve));
  assert.equal(p.error.value,'邀请已失效');
  assert.equal(p.invitation.value,null);
  await p.accept();
  assert.equal(accepts,0);
});

test('成员页不会自动生成邀请，只有明确分享按钮才能携带邀请令牌', async () => {
  let issued=0;
  const p=page('companions', { companion: {
    list: async () => ({people:[],invitations:issued ? [{id:12}] : [],currentRole:'OWNER'}),
    invite: async () => { issued++; return {id:12,token:'private-token',expiresAt:'2099-01-01T00:00:00Z'}; }
  } }, 'createInvite, link');
  p.load({id:7}); await new Promise(resolve => setImmediate(resolve));
  assert.equal(issued,0);
  assert.equal(p.share({from:'menu'}).path,'/pages/home/home');
  await Promise.all([p.createInvite(),p.createInvite()]);
  assert.equal(issued,1);
  assert.equal(p.share({from:'menu'}).path,'/pages/home/home');
  assert.equal(p.share({from:'button'}).path,'/pages/invitation/index?token=private-token');
});

test('撤销邀请会清掉准备发送的令牌，失去权限会清掉成员列表', async () => {
  let revoked=false, denied=false;
  const p=page('companions', {companion:{
    list: async () => { if(denied)throw new Error('无权访问'); return {people:[{accountId:1}],invitations:revoked ? [] : [{id:12}],currentRole:'OWNER'}; },
    invite: async () => ({id:12,token:'private-token',expiresAt:'2099-01-01T00:00:00Z'}),
    revoke: async () => {revoked=true;}
  }}, 'createInvite, revoke, refresh, group, link');
  p.load({id:7}); await new Promise(resolve => setImmediate(resolve));
  await p.createInvite(); await p.revoke(12);
  assert.equal(p.link.value,null);
  assert.equal(p.share({from:'button'}).path,'/pages/home/home');
  denied=true; await p.refresh();
  assert.equal(p.group.value.people.length,0);
  assert.equal(p.group.value.currentRole,'');
});
