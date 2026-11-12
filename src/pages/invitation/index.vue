<template>
  <view class="sharing-page">
    <CustomHeader title="一起分账" />
    <view class="sharing-card">
      <template v-if="invitation">
        <text class="sharing-title">{{ invitation.ownerName }} 邀请你</text>
        <text class="calculator-name">加入「{{ invitation.calculatorName }}」</text>
        <text class="sharing-note">加入后可查看计算器及其全部账单，也可以新增账单。配置和同伴由创建者管理。你在其他计算器里的数据仍独立。</text>
        <text v-if="!invitation.alreadyJoined" class="sharing-note">给同伴看的名字</text>
        <input v-if="!invitation.alreadyJoined" v-model="displayName" class="sharing-input" maxlength="40" placeholder="填写你的名字，方便同伴辨认" />
        <button class="sharing-primary" :loading="busy" :disabled="busy" @tap="accept">{{ invitation.alreadyJoined ? '打开计算器' : '确认加入' }}</button>
      </template>
      <text v-else-if="!error" class="sharing-note">正在确认邀请…</text>
      <text v-if="error" class="sharing-error">{{ error }}</text>
      <button v-if="error" class="sharing-primary" @tap="load">重新检查邀请</button>
      <button class="home-button" @tap="goHome">返回我的计算器</button>
    </view>
  </view>
</template>
<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import CustomHeader from '@/components/CustomHeader/CustomHeader.vue';
import api from '@/api/costSharingApi.js';
const token = ref('');
const invitation = ref(null);
const busy = ref(false);
const error = ref('');
const displayName = ref('');
const goHome = () => uni.reLaunch({ url: '/pages/home/home' });
const load = async () => {
  error.value = ''; invitation.value = null;
  if (!token.value) { error.value = '邀请不完整，请让创建者重新发送'; return; }
  try {
    const [preview, account] = await Promise.all([api.companion.preview(token.value), api.auth.me()]);
    invitation.value = preview;
    displayName.value = account.displayName === '微信用户' ? '' : account.displayName;
  } catch (e) { error.value = e.message; }
};
const accept = async () => {
  if (busy.value) return;
  if (!invitation.value?.alreadyJoined && !displayName.value.trim()) { uni.showToast({ title: '请填写给同伴看的名字', icon: 'none' }); return; }
  busy.value = true;
  try {
    if (!invitation.value?.alreadyJoined) await api.auth.rename(displayName.value.trim());
    const id = await api.companion.accept(token.value);
    uni.redirectTo({ url: `/pages/calculator/index?id=${id}` });
  } catch (e) { error.value = e.message; }
  finally { busy.value = false; }
};
onLoad((options) => { token.value = options.token || ''; load(); });
</script>
<style scoped lang="scss">
@import '@/styles/sharing.scss';
.calculator-name { display: block; margin-top: 24rpx; font-size: 32rpx; color: #4c7b7c; line-height: 1.6; }
.home-button { background: transparent; color: #6e8b9a; margin-top: 24rpx; font-size: 27rpx; }
.home-button::after { border: none; }
</style>
