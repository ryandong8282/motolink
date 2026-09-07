const sessionStore = require('../../services/session');

Page({
  data: {
    session: null,
    avatarText: 'M',
  },

  onShow() {
    const session = sessionStore.requireSession();
    if (!session) return;
    this.setData({
      session,
      avatarText: session.nickname.substring(0, 1),
    });
  },

  onLogout() {
    wx.showModal({
      title: '退出开发态账号',
      content: '本地会话会被清除，后端中的 MVP 数据不会立即删除。',
      success(result) {
        if (!result.confirm) return;
        sessionStore.clearSession();
        wx.reLaunch({ url: '/pages/login/login' });
      },
    });
  },
});
