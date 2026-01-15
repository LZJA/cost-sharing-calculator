/**
 * 图片处理工具函数
 */

/**
 * 将图片文件路径转换为 Base64 字符串
 * @param {string} filePath - 图片文件路径
 * @returns {Promise<string>} Base64 字符串（包含 data:image/xxx;base64, 前缀）
 */
export const imageToBase64 = (filePath) => {
  return new Promise((resolve, reject) => {
    uni.getFileSystemManager().readFile({
      filePath: filePath,
      encoding: 'base64',
      success: (res) => {
        // 获取图片格式
        const format = filePath.split('.').pop().toLowerCase();
        const mimeType = getMimeType(format);

        // 返回完整的 Base64 字符串
        const base64String = `data:${mimeType};base64,${res.data}`;
        resolve(base64String);
      },
      fail: (err) => {
        console.error('读取图片文件失败:', err);
        reject(err);
      }
    });
  });
};

/**
 * 根据文件扩展名获取 MIME 类型
 * @param {string} format - 文件扩展名
 * @returns {string} MIME 类型
 */
const getMimeType = (format) => {
  const mimeTypes = {
    'jpg': 'image/jpeg',
    'jpeg': 'image/jpeg',
    'png': 'image/png',
    'gif': 'image/gif',
    'webp': 'image/webp',
    'bmp': 'image/bmp'
  };

  return mimeTypes[format] || 'image/jpeg';
};

/**
 * 压缩图片（可选功能）
 * @param {string} filePath - 图片文件路径
 * @param {number} quality - 压缩质量 (0-100)
 * @returns {Promise<string>} 压缩后的临时文件路径
 */
export const compressImage = (filePath, quality = 80) => {
  return new Promise((resolve, reject) => {
    uni.compressImage({
      src: filePath,
      quality: quality,
      success: (res) => {
        resolve(res.tempFilePath);
      },
      fail: (err) => {
        console.error('压缩图片失败:', err);
        reject(err);
      }
    });
  });
};
