<template>
  <header class="topbar">
    <div>
      <div class="title">日志与安全中心</div>
      <div class="meta">登录日志、关键操作日志、敏感操作二次验证、密码策略</div>
    </div>
    <div class="actions">
      <button class="btn">导出审计日志</button>
      <button class="btn primary">启用二次验证</button>
    </div>
  </header>

  <section class="grid">
    <article class="card span-4">
      <h3>安全状态</h3>
      <p class="small">密码算法：BCrypt</p>
      <p class="small">登录防刷：验证码 + 失败锁定</p>
      <p class="small">会话机制：JWT</p>
      <div class="actions">
        <span class="badge ok">BCrypt 已启用</span>
        <span class="badge ok">JWT 正常</span>
        <span class="badge warn">二次验证部分接口未覆盖</span>
      </div>
    </article>

    <article class="card span-8">
      <h3>登录日志</h3>
      <table class="table">
        <thead><tr><th>时间</th><th>用户名</th><th>IP</th><th>结果</th><th>设备</th></tr></thead>
        <tbody>
          <tr><td>2026-03-09 13:35</td><td>smile</td><td>127.0.0.1</td><td><span class="badge ok">成功</span></td><td>macOS Chrome</td></tr>
          <tr><td>2026-03-09 13:33</td><td>ops_li</td><td>10.12.2.7</td><td><span class="badge danger">失败</span></td><td>Windows Edge</td></tr>
        </tbody>
      </table>
    </article>

    <article class="card span-12">
      <h3>关键操作日志</h3>
      <table class="table">
        <thead><tr><th>操作时间</th><th>操作人</th><th>模块</th><th>动作</th><th>详情</th></tr></thead>
        <tbody>
          <tr><td>2026-03-09 14:06</td><td>smile</td><td>用户管理</td><td>重置密码</td><td>重置用户密码并要求首次登录修改</td></tr>
          <tr><td>2026-03-09 14:02</td><td>smile</td><td>角色权限</td><td>更新权限</td><td>新增 user:export 权限</td></tr>
          <tr><td>2026-03-09 13:58</td><td>auditor</td><td>组织管理</td><td>删除组织</td><td>触发二次验证后删除测试部门</td></tr>
        </tbody>
      </table>
    </article>
  </section>
</template>
