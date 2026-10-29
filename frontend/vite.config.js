import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Le port 5173 est celui autorisé par la configuration CORS du backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
  },
});
