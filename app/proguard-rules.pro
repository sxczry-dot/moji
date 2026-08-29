# Room 数据库实体字段被生成代码访问，显式保留
-keep class com.autobook.app.data.Transaction { *; }

# 小组件和广播接收器由系统通过反射实例化（manifest 声明的组件 R8 默认保留）
# 以下为保险规则
-keep class com.autobook.app.widget.BudgetWidgetProvider { *; }
-keep class com.autobook.app.service.** { *; }
