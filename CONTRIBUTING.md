# Contributing

## 分支约定

- `feat/*`：功能
- `fix/*`：缺陷
- `chore/*`：工程与依赖
- `docs/*`：文档

## 提交前检查

```bash
make api-test
make admin-build
make mobile-test
```

不要把 RTC Secret、地图 Key、短信密钥、推送证书、签名文件或真实用户数据提交到仓库。
