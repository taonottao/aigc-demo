<template>
  <header class="topbar">
    <div>
      <div class="title">组织管理</div>
      <div class="meta">左侧组织树可展开，右侧展示当前组织的直接子组织</div>
    </div>
    <div class="actions">
      <button class="btn">拖拽排序</button>
      <button class="btn">设置父级</button>
      <button class="btn primary">新增子组织</button>
    </div>
  </header>

  <section class="split-shell">
    <article class="tree-panel">
      <h3>组织树</h3>
      <OrgTree :nodes="orgTree" :selected-id="selected.id" @select="selectNode" />
      <p class="small">当前节点：{{ selected.name }}（{{ selected.code }}）</p>
    </article>

    <article class="list-panel">
      <h3>{{ selected.name }} - 子组织列表</h3>
      <table class="table">
        <thead><tr><th>名称</th><th>编码</th><th>负责人</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in selected.children || []" :key="row.id">
            <td>{{ row.name }}</td>
            <td>{{ row.code }}</td>
            <td>{{ row.leader || '-' }}</td>
            <td><span :class="['badge', row.status === 1 ? 'ok' : 'warn']">{{ row.status === 1 ? '启用' : '禁用' }}</span></td>
            <td>编辑 | 新增子级 | 删除</td>
          </tr>
          <tr v-if="!(selected.children || []).length"><td colspan="5">当前节点暂无子组织</td></tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import OrgTree from '../components/OrgTree.vue'

const orgTree = [
  {
    id: 1,
    name: '集团总部',
    code: 'HQ',
    leader: '系统管理员',
    status: 1,
    children: [
      {
        id: 2,
        name: '研发中心',
        code: 'RD-001',
        leader: '王工',
        status: 1,
        children: [
          { id: 3, name: '平台组', code: 'RD-PLAT', leader: '陈工', status: 1, children: [] },
          { id: 4, name: '应用组', code: 'RD-APP', leader: '李工', status: 1, children: [] }
        ]
      },
      { id: 5, name: '华东运营', code: 'OPS-EAST', leader: '周工', status: 1, children: [] }
    ]
  }
]

const selected = ref(orgTree[0].children[0])

function selectNode(node) {
  selected.value = node
}
</script>
