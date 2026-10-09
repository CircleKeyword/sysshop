# sysshop（以物易物）

**作者：** CircleKeyword · **版本：** `0.0.1` · **加载器：** Minecraft Forge  
Forge 加载器中显示为 **sysshop**；打开模组主商店界面后显示 **“以物易物”**。

sysshop 是一个 Forge 换物商店模组，提供玩家商店、多人游戏系统商店、商店管理与补货 GUI。四个 Minecraft 版本分别维护为独立工程。

## 功能

- 浏览商店、创建个人商店，并通过游戏内界面管理交易。
- **多人游戏：** 系统商店固定显示在商店列表首位，拥有无限供货；OP（权限等级 2 及以上）可管理系统商店。
- **单人游戏：** 不创建系统商店，可创建和管理个人商店。
- 玩家商店使用 **54 格库存**，需要店主补货；系统商店不需要补货。
- 玩家购买时，支付物品进入店铺库存，兑换物品从库存扣除。库存无法容纳整笔付款时，交易整体失败，不扣款也不发货。
- ESC 可关闭界面。

## 兼容版本

| Minecraft | Forge | Java 运行环境 | 发布文件 |
|---|---:|---:|---|
| 1.16.5 | 36.2.42 | Java 8 | [sysshop-1.16.5-0.0.1.jar](release/sysshop-1.16.5-0.0.1.jar) |
| 1.18.2 | 40.3.12 | Java 17 | [sysshop-1.18.2-0.0.1.jar](release/sysshop-1.18.2-0.0.1.jar) |
| 1.20.1 | 47.4.26 | Java 17 | [sysshop-1.20.1-0.0.1.jar](release/sysshop-1.20.1-0.0.1.jar) |
| 1.21.1 | 52.1.16 | Java 21 | [sysshop-1.21.1-0.0.1.jar](release/sysshop-1.21.1-0.0.1.jar) |

请使用与游戏版本匹配的 Forge 和 JAR，不同 Minecraft 版本的文件不能互换。本模组没有额外的模组前置。

## 安装

1. 安装表格中对应版本的 Minecraft 和 Forge。
2. 将对应 JAR 放入游戏实例的 `mods` 文件夹。
3. 启动游戏；单人游戏只需在客户端安装。
4. 多人游戏中，客户端和服务器都要安装相同 Minecraft/Forge 版本的 sysshop JAR。

## 游戏内命令

| 命令 | 作用 |
|---|---|
| `/sysshop open` | 打开“以物易物”商店界面 |
| `/sysshop create` | 创建个人商店 |
| `/sysshop manager` | 管理自己的商店；多人游戏 OP 还可管理系统商店 |

## 商店管理与交易

- 管理页左侧选择商店，交易行左侧配置支付物品，右侧配置兑换物品；最多 16 条交易行。
- 在管理页**左键**点击背包物品以选择副本，再**右键**点击交易槽上架。此操作只复制物品定义，不会扣除背包物品；没有选中物品时右键交易槽可清除配置。
- “补货”页面有 54 格仓库：右键选择背包堆叠，左键点击仓库格补入；空手左键点击仓库物品可取回。
- 交易配置仅保存物品注册表 ID 和数量，**不保存附魔、名称、NBT 或自定义数据组件**。带自定义数据的物品变体不属于支持范围。
- 玩家商店的付款物品存入该店库存；若库存无法完整接收付款，交易不会部分成交。

## 数据保存

- 店铺、交易行和库存保存于当前世界的数据目录：`data/sysshop-shops.json`。
- `config/sysshop-offers.txt` 保留作兼容的系统商店初始种子文件。系统商店初始化后，以世界数据和游戏内管理界面中的实际配置为准。

## 构建

每个版本都是独立工程。进入对应目录后运行 Gradle Wrapper：

```powershell
cd .\forge-1.20.1
.\gradlew.bat clean build --no-daemon
```

将示例中的 `forge-1.20.1` 替换为目标工程目录。构建 JDK 要求：

- 1.16.5：使用 JDK 17 构建，输出 Java 8 兼容字节码。
- 1.18.2、1.20.1：使用 JDK 17。
- 1.21.1：使用 JDK 21。

构建产物位于各工程的 `build/libs/`；发布文件集中在 `release/`。

## 验证状态

四个版本均已执行 `clean build`，构建成功。Gradle 的 `test` 任务当前为 `NO-SOURCE`；目前只有1.20.1版本完成了真是游戏客户端、服务器、GUI交互与联机交易测试，其他版本均未完成真实游戏客户端、服务器、GUI 交互和联机交易测试。使用前请在目标游戏版本中自行验证，尤其不要把当前构建状态误当作实机测试结论。

## 许可证

本项目原创代码采用 [MIT License](LICENSE)，版权署名为 CircleKey（CircleKeyword简写，QQ号369869659）。Minecraft、Forge、Gradle Wrapper 及其他第三方组件和商标各自受其上游条款约束；本项目不包含 Minecraft 游戏文件或官方资源。

## 免责声明

sysshop 是 Minecraft 社区模组，**不是 Minecraft 官方产品，未获 Mojang Studios 或 Microsoft 批准，也与其无关联**。Minecraft 为其权利人所有的商标。请勿将本项目或其发布包描述为官方或官方背书产品。

- [Minecraft EULA](https://www.minecraft.net/en-us/eula)
- [Minecraft Usage Guidelines](https://www.minecraft.net/en-us/usage-guidelines)
