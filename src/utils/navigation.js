/** 为自定义导航避开微信右上角的系统胶囊，兼容不同屏宽和状态栏。 */
export function navigationLayout(windowInfo = {}, capsule, hasSystemMenu = false) {
  const windowWidth = Number(windowInfo.windowWidth) || 375;
  const statusBarHeight = Number.isFinite(windowInfo.statusBarHeight) ? windowInfo.statusBarHeight : 20;
  const validCapsule = capsule && capsule.left > 0 && capsule.left < windowWidth && capsule.height > 0;
  const rightInset = validCapsule ? windowWidth - capsule.left + 8 : hasSystemMenu ? 104 : 0;
  const gap = validCapsule ? Math.max(0, capsule.top - statusBarHeight) : 6;
  return { statusBarHeight, navHeight: Math.max(44, validCapsule ? capsule.height + gap * 2 : 44), rightInset };
}
