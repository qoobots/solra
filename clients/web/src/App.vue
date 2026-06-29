<template>
  <div id="solra-app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// App root — layout/global state managed by router views
</script>

<style lang="scss">
#solra-app {
  min-height: 100vh;
  background: var(--solra-bg-primary);
  color: var(--solra-text-primary);
}
</style>
