<template>
  <div class="min-h-screen bg-gray-50">
    <Navbar />
    <main class="max-w-7xl mx-auto px-4 py-6">
      <router-view :key="$route.fullPath" />
    </main>
    <ToastContainer />
  </div>
</template>

<script setup>
import Navbar from './components/layout/Navbar.vue'
import ToastContainer from './components/ui/ToastContainer.vue'
</script>
