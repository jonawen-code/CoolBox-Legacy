# Changelog

All notable changes to the CoolBox project will be documented in this file.

## [1.2.16] - 2026-03-04
### Changed
- **分类图标精简 (Category Icon Cleanup)**：移除"熟食剩菜"分类前的 Emoji 图标，使分类名称更简洁。
### Fixed
- **快速添加同步 (Quick Add Sync)**：主界面快速添加按钮现在智能匹配用户自定义的分类名称，不再使用硬编码字符串。
- **分类迁移交互优化 (Migration Dialog UX)**：分类迁移对话框改为直接展示分类选择列表，移除冗余的"返回修改"按钮，操作更直观。

## [1.2.15] - 2026-03-04
### Fixed
- **分类设置一致性修复 (Setup Consistency Fix)**：基础设置中的分类输入框现在会动态加载当前设置（含 Emoji），彻底解决了因文字不匹配导致的误判“分类移除”问题。
- **迁移对话框体验优化 (Migration UI Refined)**：为分类迁移对话框增加了“返回修改”按钮，允许用户在发现误判时一键返回修正，并优化了提示文字。

## [1.2.14] - 2026-03-04
### Added
- **设置流程“智能跳过” (Smart Setup Skip)**：基础设置中，若存储设备名称未变动，可选择跳过层级设置，直接进入分类管理。
### Fixed
- **分类保存彻底修复 (Category Persistence Fix)**：修复了 `SetupActivity` 主界面与分类编辑弹窗之间的数据流漏洞，确保所有预设修改及新增分类均能成功持久化。

## [1.2.13] - 2026-03-04
### Fixed
- **标签栏显示优化 (Tab Bar Optimization)**：主界面标签栏改为仅显示基础设备名称，点击后展示该设备下所有层级的食品。
- **分类保存漏洞修复 (Category Saving Fix)**：修复了 `SetupActivity` 中新增分类无法正确持久化的逻辑漏洞。

## [1.2.12] - 2026-03-03
### Fixed
- **核心初始化顺序修复 (ViewModel Initialization Fix)**：彻底修复了 `MainViewModel` 中由于 Kotlin 属性与 `init` 块初始化顺序不当导致的 `NullPointerException` 闪退。

## [1.2.11] - 2026-03-03
### Fixed
- **多重启动闪退修复 (Multiple Crash Fixes)**：彻底解决了 `MainActivity` 中因 `TimeMachine` 及 `FontScale` 观察器初始化顺序导致的多个潜在闪退点。

## [1.2.10] - 2026-03-03
### Fixed
- **启动闪退修复 (Crash Fix)**：修复了由于 `FontScale` 观察器初始化顺序不当导致的 `MainActivity` 启动闪退问题。

## [1.2.09] - 2026-03-03
### Added
- **新图标集成 (Icon Integration)**：新增“榴莲 (Durian)”图标及其自动匹配逻辑。
- **目录更新**：在“🥦 蔬菜水果”分类中添加“榴莲”作为预设项。

### Changed
- **全端同步**：Android 与 微信小程序 同步更新至 v1.2.09。

## [1.2.08] - 2026-03-03
### Added
- **分类自定义与一键迁移 (Category Management)**：在基础设置中新增分类编辑器，支持添加、修改和删除分类。
- **智能数据迁移逻辑**：若用户修改或删除了已有分类，系统将自动提示并将相关食物条目迁移至新分类，确保数据完整性。
- **汇总视图强化 (Robust Summary)**：改进了汇总视图下的位置显示算法，现在会根据已保存的设备列表进行精确匹配，彻底解决了位置信息显示过多冗余细节的问题。

### Changed
- **数据库架构升级**：新增分类存储字段，支持更更灵活的数据维度。
- **全端版本同步**：Android 与 微信小程序 同步更新至 v1.2.08。

## [1.2.07] - 2026-03-03
### Added
- **Setup Grouping Logic**: Fixed issue where individual layers were loaded as separate devices; now grouped by base device name.
- **Summary View Simplification**: In "Summary" tab, location info now only displays the base device name (e.g., "在大冰箱") for better clarity.

### Changed
- **Visual Enhancement**: Darkened all gray text colors to `#444444` / `#555555` to improve readability.
- **Formatting Update**: Changed portion separator from "|" to "," (e.g., "1份, 1瓶/份").
- **Global Sync**: Synchronized version 1.2.07 across Android and WeChat Mini Program.

## [1.2.06] - 2026-03-03
### Added
- **Location Naming Logic**: Refined naming for deep freezers (removing redundant "freezer room" label).
- **Chinese Ordinals**: Switched layer numbering to natural Chinese (第一层, 第二层, etc.).
- **Expanded Capacity**: Re-initialization now supports up to 9 layers per compartment.

## [1.2.05] - 2026-03-03
### Added
- Created formal `CHANGELOG.md` to track project evolution.
### Changed
- Updated versioning scheme: patch version now uses two digits (e.g., 1.2.05) as requested.
- Synchronized version headers across Android and WeChat Mini Program.

## [1.2.04] - 2026-03-03
### Added
- **Location Granularity**: New guided setup flow for specific compartments (Refrigeration, Micro-freeze, Freezing).
- **Multi-layered Freezer**: Support for defining layer counts (1-6) in freezer compartments.
- **Smart Tabs**: Main UI now displays granular sub-locations as dedicated tabs.

## [1.2.03] - 2026-03-03
### Improved
- **Input Efficiency**: New "Selection-First" food entry flow with "Add new..." fallback.
- **Layout Optimization**: Narrowed action buttons (90dp -> 60dp) to reclaim horizontal space for food info area.
- **Visuals**: Multi-line button text for "Take 1 Portion" to accommodate narrower width.

## [1.2.02] - 2026-03-01
### Changed
- Shrunk item icons to 75% (60dp) for better proportions.
- Renamed "备注：" label to "注：".
- Standardized card height by maintaining a blank line for empty remarks.
### Added
- Integrated 11 new food icons (blueberries, broccoli, butter, cheese, dumpling, green apple, pepper, lemon, mango, pear, tangerine).

## [1.2.01] - 2026-03-01
### Fixed
- Restored portion count display in the item list.
- Moved remarks to a new line for better readability.
- Locked "Category" selection to read-only dropdown.
- Kept "Food Name" selection as an editable dropdown (Manual entry = New food).

## [1.2.00] - 2026-03-01
### Added
- Complete UI redesign with modern "Cool Blue" premium theme.
- Dynamic font scaling system (80% - 150%).
- Integrated Time Machine dashboard for expiration simulation.
- Icon selection system for food items.
