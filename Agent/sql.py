# -*- coding: utf-8 -*-
"""
查询用户近 10 天日程安排的 Python 实现（参考代码）

⚠️  重要说明：本代码【不能】直接在 Dify 的「代码执行」节点中运行。
    Dify 代码节点运行在隔离沙箱中：
      - 沙箱内没有 pymysql 库（import 会失败）
      - 沙箱禁止任何网络连接（即使能 import 也无法连到 MySQL）
    在 Dify 工作流中查询日程，正确做法是用「HTTP 请求」节点调用后端接口：
        GET http://host.docker.internal:8080/api/ai/recent-records?userId=<数字用户ID>
        Header: Authorization: teamsync-ai-key-2026
    本文件作为独立脚本 / 后端集成的参考实现，可在宿主机直接运行验证 SQL。

MySQL 配置与 teamsync-mvp-server/src/main/resources/application.yml 保持一致。
"""

import pymysql

# ─── MySQL 配置（来自 application.yml）──────────────────────────────
DB_CONFIG = {
    "host": "localhost",          # 后端连的是 localhost:3306
    "port": 3306,
    "user": "root",
    "password": "661213",
    "database": "teamsync_mvp",
    "charset": "utf8mb4",
}

# schedule.status: 0=未开始 1=进行中 2=已完成
STATUS_TEXT = {0: "未开始", 1: "进行中", 2: "已完成"}


def query_recent_schedules(user_id: int, days: int = 10) -> dict:
    """查询指定用户近 N 天（含今天）的日程安排，按日期倒序返回。

    Args:
        user_id: sys_user.id（数字主键，不是 Dify 的 UUID）
        days:    统计天数，默认 10

    Returns:
        {"userId", "days", "total", "completed", "pending", "schedules": [...]}
    """
    conn = pymysql.connect(**DB_CONFIG, cursorclass=pymysql.cursors.DictCursor)
    try:
        with conn.cursor() as cursor:
            sql = """
                SELECT id, user_id, title, goal_desc, status, plan_date, created_at
                FROM schedule
                WHERE user_id = %s
                  AND plan_date >= DATE_SUB(CURDATE(), INTERVAL %s - 1 DAY)
                  AND plan_date <= CURDATE()
                ORDER BY plan_date DESC, created_at DESC
            """
            cursor.execute(sql, (user_id, days))
            rows = cursor.fetchall()

        schedules = []
        for row in rows:
            schedules.append({
                "id": row["id"],
                "title": row["title"],
                "goalDesc": row["goal_desc"],
                "status": row["status"],
                "statusText": STATUS_TEXT.get(row["status"], "未知"),
                "planDate": str(row["plan_date"]),
            })

        completed = sum(1 for s in schedules if s["status"] == 2)
        return {
            "userId": user_id,
            "days": days,
            "total": len(schedules),
            "completed": completed,
            "pending": len(schedules) - completed,
            "schedules": schedules,
        }
    finally:
        conn.close()


if __name__ == "__main__":
    import json

    # 示例：查询 userId=2 的近 10 天日程（换成你自己的数字用户ID）
    result = query_recent_schedules(user_id=2, days=10)
    print(json.dumps(result, ensure_ascii=False, indent=2))
