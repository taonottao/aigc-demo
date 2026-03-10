<template>
  <ul class="org-tree">
    <li v-for="node in nodes" :key="node.id">
      <button
        class="node-btn"
        :class="{ active: selectedId === node.id }"
        @click="$emit('select', node)"
      >
        {{ node.name }}
      </button>
      <OrgTree
        v-if="node.children && node.children.length"
        class="children"
        :nodes="node.children"
        :selected-id="selectedId"
        @select="$emit('select', $event)"
      />
    </li>
  </ul>
</template>

<script setup>
defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  selectedId: {
    type: Number,
    default: null
  }
})

defineEmits(['select'])
</script>
