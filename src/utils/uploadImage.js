/**
 * 图片上传到微信云存储
 */
// 云托管服务配置
const CLOUD_CONFIG = {
  // 云托管环境ID
  env: 'prod-0gko48kec6190500',
  // 云托管服务名称（在云托管控制台查看）
  serviceName: 'cost-sharing',
}
/**
 * 上传图片到云存储
 * @param {String} tempFilePath - 临时文件路径
 * @param {String} folder - 存储文件夹名称（可选）
 * @returns {Promise<Object>} { fileID: 云文件ID, tempFileURL: 临时访问URL }
 */
export const uploadToCloud = (tempFilePath, folder = 'card-backgrounds') => {
  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '上传中...' })

    // 生成云存储文件名
    const timestamp = Date.now()
    const random = Math.floor(Math.random() * 10000)
    const ext = tempFilePath.split('.').pop() || 'jpg'
    const cloudPath = `${folder}/${timestamp}_${random}.${ext}`

    console.log('开始上传:', cloudPath)

    wx.cloud.uploadFile({
      cloudPath: cloudPath,
      filePath: tempFilePath,
      config: {
        env: CLOUD_CONFIG.env
      },
      success: res => {
        uni.hideLoading()
        console.log('上传成功:', res.fileID)
        resolve({
          fileID: res.fileID,
          cloudPath: cloudPath
        })
      },
      fail: err => {
        uni.hideLoading()
        console.error('上传失败:', err)
        uni.showToast({
          title: '上传失败，请重试',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * 获取云文件的临时访问链接
 * @param {String} fileID - 云文件ID
 * @returns {Promise<String>} 临时访问URL（有效期2小时）
 */
export const getTempFileURL = (fileID) => {
  return new Promise((resolve, reject) => {
    wx.cloud.getTempFileURL({
      fileList: [fileID],
      success: res => {
        if (res.fileList && res.fileList.length > 0) {
          const tempFileURL = res.fileList[0].tempFileURL
          console.log('获取临时链接成功:', tempFileURL)
          resolve(tempFileURL)
        } else {
          reject(new Error('获取临时链接失败'))
        }
      },
      fail: err => {
        console.error('获取临时链接失败:', err)
        reject(err)
      }
    })
  })
}

/**
 * 删除云存储文件
 * @param {String} fileID - 云文件ID
 */
export const deleteFromCloud = (fileID) => {
  return new Promise((resolve, reject) => {
    if (!fileID || !fileID.startsWith('cloud://')) {
      console.warn('无效的fileID:', fileID)
      resolve() // 不报错，继续执行
      return
    }

    wx.cloud.deleteFile({
      fileList: [fileID],
      success: res => {
        console.log('删除成功:', res)
        resolve(res)
      },
      fail: err => {
        console.error('删除失败:', err)
        // 不reject，因为删除失败不应该阻塞主流程
        resolve()
      }
    })
  })
}

/**
 * 压缩图片
 * @param {String} tempFilePath - 临时文件路径
 * @param {Number} quality - 压缩质量 0-100
 * @returns {Promise<String>} 压缩后的临时文件路径
 */
export const compressImage = (tempFilePath, quality = 50) => {
  return new Promise((resolve, reject) => {
    uni.compressImage({
      src: tempFilePath,
      quality: quality,
      success: res => {
        console.log('压缩成功:', res.tempFilePath)
        resolve(res.tempFilePath)
      },
      fail: err => {
        console.error('压缩失败:', err)
        // 压缩失败不影响上传，直接返回原图
        resolve(tempFilePath)
      }
    })
  })
}

/**
 * 上传图片到云存储（带压缩）
 * @param {String} tempFilePath - 临时文件路径
 * @param {Object} options - 配置项
 * @returns {Promise<Object>} { fileID: 云文件ID, cloudPath: 云路径 }
 */
export const uploadImageWithCompress = async (tempFilePath, options = {}) => {
  const {
    folder = 'card-backgrounds',
    quality = 50,
    needCompress = true
  } = options

  try {
    // 1. 压缩图片（可选）
    let finalPath = tempFilePath
    if (needCompress) {
      console.log('压缩图片...')
      finalPath = await compressImage(tempFilePath, quality)
    }

    // 2. 上传到云存储
    console.log('上传到云存储...')
    const result = await uploadToCloud(finalPath, folder)

    return result
  } catch (error) {
    console.error('上传流程失败:', error)
    throw error
  }
}

