<template>
  <view class="sharing-page">
    <CustomHeader title="我的账号" />
    <view class="sharing-card">
      <text class="sharing-title">给同伴看的名字</text>
      <text class="sharing-note">使用当前微信账号登录。这个名字用于同伴列表，和参与分账的人员名字相互独立。</text>
      <input v-model="displayName" class="sharing-input" maxlength="40" placeholder="例如：李子" />
      <button class="sharing-primary" :loading="busy" :disabled="busy || !account" @tap="save">保存名字</button>
      <text v-if="account" class="sharing-note">账号编号：{{ account.id }}</text>
      <text v-if="error" class="sharing-error">{{ error }}</text>
    </view>
    <view class="sharing-card">
      <text class="sharing-title">旧数据迁移</text>
      <text class="sharing-note">仅历史计算器的管理员需要此功能。复制自己的微信账号标识，供管理员在服务器上绑定历史数据。昵称相同不会获得访问权限。</text>
      <button class="identity-button" :disabled="!account" @tap="copyIdentity">复制我的迁移标识</button>
    </view>
  </view>
</template>
<script setup>
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import CustomHeader from '@/components/CustomHeader/CustomHeader.vue';
import api from '@/api/costSharingApi.js';
const account = ref(null);
const displayName = ref('');
const error = ref('');
const busy = ref(false);
onLoad(async () => {
  try { account.value = await api.auth.me(); displayName.value = account.value.displayName; }
  catch (e) { error.value = e.message; }
});
const save = async () => {
  if (busy.value) return;
  if (!displayName.value.trim()) { uni.showToast({ title: '请填写名字', icon: 'none' }); return; }
  busy.value = true;
  try { account.value = await api.auth.rename(displayName.value.trim()); error.value = ''; uni.showToast({ title: '已保存', icon: 'success' }); }
  catch (e) { error.value = e.message; }
  finally { busy.value = false; }
};
const copyIdentity = async () => {
  try { const identity = await api.auth.identity(); uni.setClipboardData({ data: identity.openid }); }
  catch (e) { error.value = e.message; }
};
</script>
<style scoped lang="scss">
@import '@/styles/sharing.scss';
.identity-button { color: #537e8c; background: #eef5f5; line-height: 80rpx; font-size: 27rpx; border-radius: 18rpx; }
.identity-button::after { border: none; }
</style>
