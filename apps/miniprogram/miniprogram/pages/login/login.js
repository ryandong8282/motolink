const api = require('../../services/api');
const sessionStore = require('../../services/session');

Page({
  data: {
    nickname: '',
    submitting: false,
  },

  onLoad() {
    if (sessionStore.getSession()) {
      wx.switchTab({ url: '/pages/home/home' });
    }
  },

  onNicknameInput(event) {
    this.setData({ nickname: event.detail.value });
  },

  async onSubmit() {
    const nickname = this.data.nickname.trim();
    if (nickname.length < 2) {
      wx.showToast({ title: '请输入至少 2 个字符', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      const session = await api.devLogin(nickname);
      sessionStore.saveSession(session);
      wx.switchTab({ url: '/pages/home/home' });
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
