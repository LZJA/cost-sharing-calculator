import { readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const directory = new URL('../dist/build/mp-weixin/', import.meta.url);
const api = readFileSync(new URL('api/costSharingApi.js', directory), 'utf8');
if (!api.includes('https://cost.higheq.top/api')) throw new Error('发行包缺少公网接口地址，请重新构建');
if (api.includes('http://localhost:8080/api')) throw new Error('发行包仍包含本地接口分支，不能提交审核');
if (api.includes('callContainer')) throw new Error('发行包仍在调用旧云托管接口，不能提交审核');
const configPath = new URL('project.config.json', directory);
const config = JSON.parse(readFileSync(configPath, 'utf8'));
if (config.appid !== 'wx443fb670b28db0aa') throw new Error('发行包 AppID 与项目不符');
config.setting = { ...config.setting, urlCheck: true };
writeFileSync(configPath, JSON.stringify(config, null, 2) + '\n');
console.log('发行包检查通过：公网接口、AppID 正确，已开启域名校验。');
console.log(`上传目录：${fileURLToPath(directory)}`);
