<template>
  <view class="sharing-page">
    <CustomHeader title="同伴" />
    <view class="sharing-card">
      <text class="sharing-title">一起记账的人</text>
      <text class="sharing-note">这里管理访问权限，不会改变分账人员或历史金额。同伴可以查看和新增账单；创建者管理配置、同伴及账单删除。</text>
      <view v-for="person in group.people" :key="person.accountId" class="person-row">
        <view class="person-info">
          <text class="person-avatar">{{ person.displayName ? [...person.displayName][0] : "友" }}</text>
          <view class="person-text">
            <text class="person-name">{{ person.displayName }}</text>
            <text class="person-role">{{ person.role === 'OWNER' ? '创建者' : '同伴' }}</text>
          </view>
        </view>
        <button v-if="group.currentRole === 'OWNER' && person.role !== 'OWNER'" class="text-button danger" @tap="remove(person)">移除</button>
      </view>
      <text v-if="error" class="sharing-error" @tap="refresh">{{ error }} · 点击重试</text>
    </view>
    <view v-if="group.currentRole === 'OWNER'" class="sharing-card">
      <text class="sharing-title">邀请新同伴</text>
      <text class="sharing-note">每份邀请限一人加入，7 天内有效。对方打开微信邀请卡片并确认后，才能看到计算器和全部共同账单。</text>
      <button class="sharing-primary" :loading="busy" :disabled="busy" @tap="createInvite">生成一份新邀请</button>
      <view v-if="link" class="ready-invite"><text>邀请已准备好，发送给一位同伴</text><button class="sharing-primary" open-type="share" data-invite="true">发送微信邀请</button></view>
      <view v-for="invite in group.invitations" :key="invite.id" class="person-row">
        <view><text class="person-name">待使用的邀请 #{{ invite.id }}</text><text class="person-role">{{ expiry(invite.expiresAt) }} 到期</text></view>
        <button class="text-button" @tap="revoke(invite.id)">撤销</button>
      </view>
      <text class="sharing-note">移除同伴会同时撤销所有未使用邀请。之后可重新生成邀请。</text>
    </view>
  </view>
</template>
<script setup>
import { ref } from 'vue';
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app';
import CustomHeader from '@/components/CustomHeader/CustomHeader.vue';
import api from '@/api/costSharingApi.js';
const calculatorId = ref('');
const group = ref({ people: [], invitations: [], currentRole: '' });
const link = ref(null);
const busy = ref(false);
const error = ref('');
const expiry = (value) => value ? value.slice(0, 10) : '';
const refresh = async () => {
  try {
    group.value = await api.companion.list(calculatorId.value);
    error.value = '';
    if (link.value && !group.value.invitations.some(i => i.id === link.value.id)) link.value = null;
  } catch (e) { group.value = { people: [], invitations: [], currentRole: '' }; link.value = null; error.value = e.message; }
};
const createInvite = async () => {
  if (busy.value) return;
  busy.value = true;
  try { link.value = await api.companion.invite(calculatorId.value); await refresh(); }
  catch (e) { error.value = e.message; }
  finally { busy.value = false; }
};
const revoke = async (id) => {
  try { await api.companion.revoke(calculatorId.value, id); if (link.value?.id === id) link.value = null; await refresh(); }
  catch (e) { error.value = e.message; }
};
const remove = (person) => uni.showModal({ title: '移除同伴', content: `移除“${person.displayName}”后，对方将无法再查看或新增账单，历史账单保留。所有未使用邀请也会撤销。`,
  success: async (res) => {
    if (!res.confirm) return;
    try { await api.companion.remove(calculatorId.value, person.accountId); link.value = null; await refresh(); }
    catch (e) { error.value = e.message; }
  }
});
onLoad((options) => { calculatorId.value = options.id || ''; if (calculatorId.value) refresh(); });
onShareAppMessage((event) => {
  if (event.from === 'button' && link.value && Date.parse(link.value.expiresAt) > Date.now()) return {
    title: '邀请你一起分账记账', path: `/pages/invitation/index?token=${encodeURIComponent(link.value.token)}`
  };
  return { title: '分账计算器', path: '/pages/home/home' };
});
</script>
<style scoped lang="scss">
@import '@/styles/sharing.scss';
.person-row { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; padding: 28rpx 0; border-bottom: 1rpx solid #edf1f2; }
.person-info { display: flex; align-items: center; gap: 20rpx; min-width: 0; flex: 1; }
.person-avatar { flex: 0 0 72rpx; width: 72rpx; height: 72rpx; line-height: 72rpx; text-align: center; border-radius: 24rpx; color: #4a7d80; background: #e6f3f0; font-size: 30rpx; }
.person-text { display: flex; align-items: baseline; gap: 16rpx; min-width: 0; }
.person-name { font-size: 30rpx; line-height: 42rpx; color: #436674; white-space: nowrap; }
.person-role { color: #8b9ea6; font-size: 23rpx; line-height: 34rpx; white-space: nowrap; }
.text-button { flex-shrink: 0; padding: 12rpx 0 12rpx 20rpx; background: transparent; color: #5c8796; font-size: 25rpx; line-height: 1.5; margin: 0; }
.text-button::after { border: none; }
.danger { color: #bc737c; }
.ready-invite { margin-top: 24rpx; background: #edf6f3; padding: 24rpx; border-radius: 20rpx; color: #4d8278; font-size: 25rpx; }
</style>
