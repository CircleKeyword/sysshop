# sysshop Forge 1.16.5 — 以物易物

Forge 加载器显示名与内部 ID 均为 `sysshop`；游戏内主商店界面名称为“以物易物”。作者 `CircleKey`，版本 `0.0.1`，Forge 36.2.42，Java 8 运行目标，MIT 许可证见 `LICENSE`。

## 命令

- `/sysshop open` 打开商店
- `/sysshop create` 创建个人商店
- `/sysshop manager` 管理自己的商店；多人游戏 OP 还可管理系统商店
- ESC 关闭界面

多人模式系统商店置首且无限供货；单人模式无系统商店。个人商店需从“补货”页补入物品，库存容量 54 格。买家支付物进入店铺仓库，仓库满时交易整笔失败。管理页左键背包物品选择复制（不扣物），再右键交易槽上架；未选物时右键交易槽清除。交易只保存注册表 ID/数量，不保存 NBT。

世界数据位于 `data/sysshop-shops.json`。兼容初始系统商店种子的 `config/sysshop-offers.txt` 保留原路径。

## 构建

```powershell
gradlew.bat clean build --no-daemon
```

本机提供的 Java 8 目录没有 `javac`，使用 JDK 17 编译器并输出 Java 8 兼容字节码。构建目标 JAR：`../release/sysshop-1.16.5-0.0.1.jar`。Gradle 测试为 `NO-SOURCE`；未做真实游戏客户端/服务器 GUI 实测。
