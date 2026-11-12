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
</script>
<style scoped lang="scss">
@import '@/styles/sharing.scss';
</style>
